package br.com.loomi.orders.service.event;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.domain.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for publishing order-related events to Kafka.
 * Handles various order lifecycle events and notifications.
 */
@Service
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topic;
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);


    /**
     * Constructs the event publisher with Kafka template and topic name.
     *
     * @param kafkaTemplate the Kafka template
     * @param topic the Kafka topic name
     */
    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                               @Value("${app.kafka.order-events-topic:order-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publishes an order created event.
     *
     * @param order the created order
     */
    public void publishOrderCreated(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("customerId", order.getCustomerId());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("status", order.getStatus().name());

        OrderEvent event = OrderEvent.of("ORDER_CREATED", payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    /**
     * Publishes an order processed event.
     *
     * @param order the processed order
     */
    public void publishOrderProcessed(Order order) {
        Map<String, Object> payload = Map.of(
                "orderId", order.getId().toString(),
                "processedAt", order.getUpdatedAt()
        );
        OrderEvent event = OrderEvent.of("ORDER_PROCESSED", payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    /**
     * Publishes an order failed event.
     *
     * @param order the failed order
     * @param reason the failure reason
     * @param message the failure message
     */
    public void publishOrderFailed(Order order, OrderFailureReason reason, String message) {
        Map<String, Object> payload = Map.of(
                "orderId", order.getId().toString(),
                "reason", reason.name(),
                "failedAt", order.getUpdatedAt(),
                "message", message
        );
        OrderEvent event = OrderEvent.of("ORDER_FAILED", payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    /**
     * Publishes an order pending approval event.
     *
     * @param order the order pending approval
     */
    public void publishOrderPendingApproval(Order order) {
        Map<String, Object> payload = Map.of(
                "orderId", order.getId().toString(),
                "status", order.getStatus().name()
        );
        OrderEvent event = OrderEvent.of("ORDER_PENDING_APPROVAL", payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    public void publishLowStockAlert(String productId, int remainingStock) {
        Map<String, Object> payload = Map.of(
                "productId", productId,
                "remainingStock", remainingStock
        );
        OrderEvent event = OrderEvent.of("LOW_STOCK_ALERT", payload);

        log.warn("LOW_STOCK_ALERT event published - productId={}, remainingStock={}", productId, remainingStock);

        kafkaTemplate.send(topic, "stock-" + productId, event);
    }

    public void publishFraudAlert(String orderId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "orderId", orderId,
                "amount", amount
        );
        OrderEvent event = OrderEvent.of("FRAUD_ALERT", payload);

        log.warn("FRAUD_ALERT event published - orderId={}, amount={}", orderId, amount);

        kafkaTemplate.send(topic, orderId, event);
    }
}
