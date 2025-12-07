package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;

/**
 * Interface for processing specific types of order items.
 * Implementations handle product-type-specific validation and business logic.
 */
public interface OrderItemProcessor {

    /**
     * Processes an individual order item with product-specific logic.
     *
     * @param order the parent order
     * @param item the order item to process
     * @param context the processing context for tracking state
     */
    void process(Order order, OrderItem item, OrderProcessingContext context);
}
