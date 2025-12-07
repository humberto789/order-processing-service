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

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);

    private static final String FIELD_ORDER_ID = "orderId";
    private static final String FIELD_CUSTOMER_ID = "customerId";
    private static final String FIELD_TOTAL_AMOUNT = "totalAmount";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_PROCESSED_AT = "processedAt";
    private static final String FIELD_REASON = "reason";
    private static final String FIELD_FAILED_AT = "failedAt";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_PRODUCT_ID = "productId";
    private static final String FIELD_REMAINING_STOCK = "remainingStock";
    private static final String FIELD_AMOUNT = "amount";

    private static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_ORDER_PROCESSED = "ORDER_PROCESSED";
    private static final String EVENT_ORDER_FAILED = "ORDER_FAILED";
    private static final String EVENT_ORDER_PENDING_APPROVAL = "ORDER_PENDING_APPROVAL";
    private static final String EVENT_LOW_STOCK_ALERT = "LOW_STOCK_ALERT";
    private static final String EVENT_FRAUD_ALERT = "FRAUD_ALERT";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topic;

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
        payload.put(FIELD_ORDER_ID, order.getId().toString());
        payload.put(FIELD_CUSTOMER_ID, order.getCustomerId());
        payload.put(FIELD_TOTAL_AMOUNT, order.getTotalAmount());
        payload.put(FIELD_STATUS, order.getStatus().name());

        OrderEvent event = OrderEvent.of(EVENT_ORDER_CREATED, payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    /**
     * Publishes an order processed event.
     *
     * @param order the processed order
     */
    public void publishOrderProcessed(Order order) {
        Map<String, Object> payload = Map.of(
                FIELD_ORDER_ID, order.getId().toString(),
                FIELD_PROCESSED_AT, order.getUpdatedAt()
        );
        OrderEvent event = OrderEvent.of(EVENT_ORDER_PROCESSED, payload);
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
                FIELD_ORDER_ID, order.getId().toString(),
                FIELD_REASON, reason.name(),
                FIELD_FAILED_AT, order.getUpdatedAt(),
                FIELD_MESSAGE, message
        );
        OrderEvent event = OrderEvent.of(EVENT_ORDER_FAILED, payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    /**
     * Publishes an order pending approval event.
     *
     * @param order the order pending approval
     */
    public void publishOrderPendingApproval(Order order) {
        Map<String, Object> payload = Map.of(
                FIELD_ORDER_ID, order.getId().toString(),
                FIELD_STATUS, order.getStatus().name()
        );
        OrderEvent event = OrderEvent.of(EVENT_ORDER_PENDING_APPROVAL, payload);
        kafkaTemplate.send(topic, order.getId().toString(), event);
    }

    /**
     * Publishes a low stock alert event.
     *
     * @param productId the ID of the product with low stock
     * @param remainingStock the remaining stock quantity
     */
    public void publishLowStockAlert(String productId, int remainingStock) {
        Map<String, Object> payload = Map.of(
                FIELD_PRODUCT_ID, productId,
                FIELD_REMAINING_STOCK, remainingStock
        );
        OrderEvent event = OrderEvent.of(EVENT_LOW_STOCK_ALERT, payload);

        LOGGER.warn("LOW_STOCK_ALERT event published - productId={}, remainingStock={}", productId, remainingStock);

        kafkaTemplate.send(topic, "stock-" + productId, event);
    }

    /**
     * Publishes a fraud alert event.
     *
     * @param orderId the ID of the order flagged for fraud
     * @param amount the amount involved in the fraudulent order
     */
    public void publishFraudAlert(String orderId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                FIELD_ORDER_ID, orderId,
                FIELD_AMOUNT, amount
        );
        OrderEvent event = OrderEvent.of(EVENT_FRAUD_ALERT, payload);

        LOGGER.warn("FRAUD_ALERT event published - orderId={}, amount={}", orderId, amount);

        kafkaTemplate.send(topic, orderId, event);
    }
}
