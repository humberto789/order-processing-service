package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.domain.enums.ProductType;
import br.com.loomi.orders.service.supporting.SubscriptionService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processor for subscription product order items.
 * Handles subscription validation and activation logic.
 */
@Component
public class SubscriptionOrderItemProcessor implements OrderItemProcessor {

    private final SubscriptionService subscriptionService;

    /**
     * Constructs the processor with the required service.
     *
     * @param subscriptionService the subscription service
     */
    public SubscriptionOrderItemProcessor(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Processes a subscription product order item.
     * Validates subscription compatibility and activates the subscription.
     *
     * @param order the parent order
     * @param item the order item to process
     * @param context the processing context
     */
    @Override
    public void process(Order order, OrderItem item, OrderProcessingContext context) {
        List<String> subscriptionProducts = order.getItems().stream()
                .filter(i -> i.getProductType() == ProductType.SUBSCRIPTION)
                .map(OrderItem::getProductId)
                .toList();

        boolean hasEnterprise = subscriptionProducts.stream()
                .anyMatch(p -> p.startsWith("SUB-ENTERPRISE"));

        boolean hasBasicOrPremium = subscriptionProducts.stream()
                .anyMatch(p -> p.startsWith("SUB-BASIC") || p.startsWith("SUB-PREMIUM"));

        if (hasEnterprise && hasBasicOrPremium) {
            context.setFailureReason(OrderFailureReason.INCOMPATIBLE_SUBSCRIPTIONS);
            context.setFailureMessage(
                    "Customer cannot have Enterprise and Basic/Premium subscriptions simultaneously"
            );
            return;
        }

        subscriptionService.validateAndActivate(order.getCustomerId(), item.getProductId());
    }
}
