package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.service.supporting.PreOrderService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Processor for pre-order product order items.
 * Handles pre-order slot reservation and discount application.
 */
@Component
public class PreOrderItemProcessor implements OrderItemProcessor {

    private final PreOrderService preOrderService;

    /**
     * Constructs the processor with the required service.
     *
     * @param preOrderService the pre-order service
     */
    public PreOrderItemProcessor(PreOrderService preOrderService) {
        this.preOrderService = preOrderService;
    }

    /**
     * Processes a pre-order product order item.
     * Validates and reserves pre-order slots, applies discounts if applicable.
     *
     * @param order the parent order
     * @param item the order item to process
     * @param context the processing context
     */
    @Override
    public void process(Order order, OrderItem item, OrderProcessingContext context) {
        preOrderService.validateAndReserve(item.getProductId(), item.getQuantity());

        Map<String, Object> metadata = item.getMetadata();
        if (metadata != null && metadata.containsKey("preOrderDiscount")) {
            BigDecimal discountPercent = new BigDecimal(metadata.get("preOrderDiscount").toString());
            BigDecimal originalTotal = item.getTotalPrice();
            BigDecimal discountAmount = originalTotal.multiply(discountPercent);
            BigDecimal newTotal = originalTotal.subtract(discountAmount);

            item.setTotalPrice(newTotal);
            order.setTotalAmount(order.getTotalAmount().subtract(discountAmount));
        }
    }
}
