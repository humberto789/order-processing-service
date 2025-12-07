package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.domain.enums.OrderStatus;
import br.com.loomi.orders.domain.enums.ProductType;
import br.com.loomi.orders.domain.event.OrderEvent;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.persistence.OrderRepository;
import br.com.loomi.orders.service.event.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for orchestrating order processing.
 * Coordinates validation, processing, and status updates for orders.
 */
@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;
    private final Map<ProductType, OrderItemProcessor> processorByType = new EnumMap<>(ProductType.class);
    private final OrderEventPublisher eventPublisher;

    /**
     * Constructs the processing service with required dependencies.
     * Automatically registers all order item processors by product type.
     *
     * @param orderRepository the order repository
     * @param processors list of all order item processors
     * @param eventPublisher the event publisher
     */
    public OrderProcessingService(OrderRepository orderRepository,
                                  List<OrderItemProcessor> processors,
                                  OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;

        for (OrderItemProcessor p : processors) {
            String name = p.getClass().getSimpleName();
            if (name.startsWith("Physical")) {
                processorByType.put(ProductType.PHYSICAL, p);
            } else if (name.startsWith("Subscription")) {
                processorByType.put(ProductType.SUBSCRIPTION, p);
            } else if (name.startsWith("Digital")) {
                processorByType.put(ProductType.DIGITAL, p);
            } else if (name.startsWith("PreOrder")) {
                processorByType.put(ProductType.PRE_ORDER, p);
            } else if (name.startsWith("Corporate")) {
                processorByType.put(ProductType.CORPORATE, p);
            }
        }
    }

    /**
     * Processes an ORDER_CREATED event.
     * Applies business rules, processes each item, and updates order status.
     *
     * @param event the order created event
     */
    @Transactional
    public void processOrderCreated(OrderEvent event) {
        Map<String, Object> payload = event.getPayload();
        Long orderId = Long.valueOf(payload.get("orderId").toString());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "ORDER_NOT_FOUND",
                        "Order %s not found".formatted(orderId)
                ));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} already processed with status {}", order.getId(), order.getStatus());
            return;
        }

        OrderProcessingContext context = new OrderProcessingContext();
        context.setTotalAmount(order.getTotalAmount());

        applyGlobalRules(order, context);

        try {
            for (OrderItem item : order.getItems()) {
                ProductType type = item.getProductType();
                OrderItemProcessor processor = processorByType.get(type);
                if (processor == null) {
                    throw new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "UNSUPPORTED_TYPE",
                            "Unsupported product type " + type
                    );
                }
                processor.process(order, item, context);
            }

            if (context.isPendingApproval()) {
                order.markPendingApproval(context.getFailureMessage());
                orderRepository.save(order);
                eventPublisher.publishOrderPendingApproval(order);
                return;
            }

            if (context.getFailureReason() != null) {
                order.markFailed(context.getFailureReason(), context.getFailureMessage());
                orderRepository.save(order);
                eventPublisher.publishOrderFailed(
                        order,
                        context.getFailureReason(),
                        context.getFailureMessage()
                );
                return;
            }

            order.markProcessed();
            orderRepository.save(order);
            eventPublisher.publishOrderProcessed(order);

        } catch (BusinessException ex) {
            log.warn("Business error while processing order {} - code={}, message={}",
                    order.getId(), ex.getCode(), ex.getMessage());

            OrderFailureReason reason;
            try {
                reason = OrderFailureReason.valueOf(ex.getCode());
            } catch (IllegalArgumentException e) {
                reason = OrderFailureReason.INVALID_REQUEST;
            }

            order.markFailed(reason, ex.getMessage());
            orderRepository.save(order);
            eventPublisher.publishOrderFailed(order, reason, ex.getMessage());

        } catch (Exception ex) {
            log.error("Unexpected error while processing order {}", order.getId(), ex);

            order.markFailed(OrderFailureReason.PAYMENT_FAILED, "Unexpected processing error");
            orderRepository.save(order);
            eventPublisher.publishOrderFailed(order, OrderFailureReason.PAYMENT_FAILED, ex.getMessage());
        }
    }

    private void applyGlobalRules(Order order, OrderProcessingContext context) {
        BigDecimal total = order.getTotalAmount();

        if (total.compareTo(BigDecimal.valueOf(10_000)) > 0) {
            context.setHighValue(true);
        }

        if (total.compareTo(BigDecimal.valueOf(20_000)) > 0) {
            if (Math.random() < 0.05) {
                context.setFraudAlert(true);
                context.setFailureReason(OrderFailureReason.FRAUD_ALERT);
                context.setFailureMessage("Fraud alert triggered");

                eventPublisher.publishFraudAlert(order.getId().toString(), total);
            }
        }

        if (Math.random() < 0.02) {
            context.setFailureReason(OrderFailureReason.PAYMENT_FAILED);
            context.setFailureMessage("Payment simulation failed");
        }
    }
}
