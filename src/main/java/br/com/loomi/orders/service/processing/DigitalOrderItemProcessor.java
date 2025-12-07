package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.service.supporting.DigitalLicenseService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Processor for digital product order items.
 * Handles license allocation and generation.
 */
@Component
public class DigitalOrderItemProcessor implements OrderItemProcessor {

    private final DigitalLicenseService digitalLicenseService;

    /**
     * Constructs the processor with the required service.
     *
     * @param digitalLicenseService the digital license service
     */
    public DigitalOrderItemProcessor(DigitalLicenseService digitalLicenseService) {
        this.digitalLicenseService = digitalLicenseService;
    }

    /**
     * Processes a digital product order item.
     * Allocates licenses and generates license keys.
     *
     * @param order the parent order
     * @param item the order item to process
     * @param context the processing context
     */
    @Override
    public void process(Order order, OrderItem item, OrderProcessingContext context) {
        digitalLicenseService.allocateLicense(order.getCustomerId(), item.getProductId(), item.getQuantity());

        Map<String, Object> metadata = item.getMetadata();
        if (metadata != null) {
            metadata.put("licenseKey", UUID.randomUUID().toString());
            metadata.putIfAbsent("deliveryEmail", "noreply@example.com");
        }
    }
}
