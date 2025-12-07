package br.com.loomi.orders.service.event;

import br.com.loomi.orders.domain.event.OrderEvent;
import br.com.loomi.orders.service.processing.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events.
 * Listens to order-related events and triggers processing logic.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderProcessingService orderProcessingService;

    /**
     * Constructs the event consumer with the required processing service.
     *
     * @param orderProcessingService the order processing service
     */
    public OrderEventConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * Handles incoming order events from Kafka.
     * Processes ORDER_CREATED events by triggering order processing.
     *
     * @param event the order event
     */
    @KafkaListener(
            topics = "${app.kafka.order-events-topic:order-events}",
            containerFactory = "orderEventListenerContainerFactory"
    )
    public void onMessage(OrderEvent event) {
        if (!"ORDER_CREATED".equals(event.getEventType())) {
            return;
        }
        log.info("Received ORDER_CREATED event {}", event.getPayload());
        orderProcessingService.processOrderCreated(event);
    }
}
