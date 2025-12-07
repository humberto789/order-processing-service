package br.com.loomi.orders.service;

import br.com.loomi.orders.domain.dto.*;
import br.com.loomi.orders.domain.dto.*;
import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.persistence.OrderRepository;
import br.com.loomi.orders.service.catalog.ProductCatalogService;
import br.com.loomi.orders.service.catalog.ProductInfo;
import br.com.loomi.orders.service.event.OrderEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for order management operations.
 * Handles order creation, retrieval, and business logic coordination.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductCatalogService catalogService;
    private final OrderEventPublisher eventPublisher;

    /**
     * Constructs the order service with required dependencies.
     *
     * @param orderRepository the order repository
     * @param catalogService the product catalog service
     * @param eventPublisher the event publisher
     */
    public OrderService(OrderRepository orderRepository,
                        ProductCatalogService catalogService,
                        OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new order from the given request.
     *
     * @param request the order creation request
     * @return the created order response
     */
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "EMPTY_ITEMS", "Order must contain at least one item");
        }

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.markPending();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemReq : request.getItems()) {
            ProductInfo product = catalogService.getRequiredProduct(itemReq.getProductId());

            OrderItem item = new OrderItem();
            item.setProductId(product.getProductId());
            item.setProductType(product.getProductType());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(product.getPrice());

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            item.setTotalPrice(itemTotal);

            item.setMetadata(itemReq.getMetadata());

            order.addItem(item);
            total = total.add(itemTotal);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        eventPublisher.publishOrderCreated(saved);

        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(saved.getId());
        response.setStatus(saved.getStatus());
        response.setTotalAmount(saved.getTotalAmount());
        response.setCreatedAt(saved.getCreatedAt());
        return response;
    }

    /**
     * Retrieves detailed information about an order.
     *
     * @param id the order identifier
     * @return the order detail response
     */
    public OrderDetailResponse getOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "Order %s not found".formatted(id)));

        OrderDetailResponse resp = new OrderDetailResponse();
        resp.setOrderId(order.getId());
        resp.setCustomerId(order.getCustomerId());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setStatus(order.getStatus());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());

        List<OrderDetailResponse.Item> items = order.getItems().stream()
                .map(i -> new OrderDetailResponse.Item(
                        i.getId(),
                        i.getProductId(),
                        i.getProductType().name(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotalPrice(),
                        i.getMetadata()
                ))
                .collect(Collectors.toList());

        resp.setItems(items);
        return resp;
    }

    /**
     * Retrieves paginated orders for a specific customer.
     *
     * @param customerId the customer identifier
     * @param pageable pagination parameters
     * @return page of order summaries
     */
    public Page<OrderSummaryResponse> getOrdersByCustomer(String customerId, Pageable pageable) {
        Page<Order> page = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        return page.map(o -> {
            OrderSummaryResponse s = new OrderSummaryResponse();
            s.setOrderId(o.getId());
            s.setTotalAmount(o.getTotalAmount());
            s.setStatus(o.getStatus());
            s.setCreatedAt(o.getCreatedAt());
            return s;
        });
    }
}
