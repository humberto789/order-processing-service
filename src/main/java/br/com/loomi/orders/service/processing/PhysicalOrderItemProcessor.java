package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.service.catalog.ProductCatalogService;
import br.com.loomi.orders.service.catalog.ProductInfo;
import br.com.loomi.orders.service.event.OrderEventPublisher;
import br.com.loomi.orders.service.supporting.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Processor for physical product order items.
 * Handles inventory reservation and stock management.
 */
@Component
public class PhysicalOrderItemProcessor implements OrderItemProcessor {

    private static final Logger log = LoggerFactory.getLogger(PhysicalOrderItemProcessor.class);

    private final ProductCatalogService catalogService;
    private final InventoryService inventoryService;
    private final OrderEventPublisher eventPublisher;

    public PhysicalOrderItemProcessor(ProductCatalogService catalogService,
                                      InventoryService inventoryService,
                                      OrderEventPublisher eventPublisher) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void process(Order order, OrderItem item, OrderProcessingContext context) {
        ProductInfo product = catalogService.getRequiredProduct(item.getProductId());

        Map<String, Object> metadata = item.getMetadata();
        String warehouseLocation = metadata != null && metadata.get("warehouseLocation") != null
                ? metadata.get("warehouseLocation").toString()
                : null;

        if (warehouseLocation != null
                && warehouseLocation.toUpperCase().startsWith("UNAVAILABLE")) {

            log.warn("WAREHOUSE_UNAVAILABLE - orderId={}, productId={}, warehouseLocation={}",
                    order.getId(), product.getProductId(), warehouseLocation);

            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "WAREHOUSE_UNAVAILABLE",
                    "Warehouse %s is currently unavailable for product %s"
                            .formatted(warehouseLocation, product.getProductId())
            );
        }

        inventoryService.initStockIfAbsent(
                product.getProductId(),
                product.getStock() != null ? product.getStock() : 0
        );

        int before = inventoryService.getStock(product.getProductId());

        inventoryService.reserve(product.getProductId(), item.getQuantity());

        int remaining = inventoryService.getStock(product.getProductId());

        log.info("PHYSICAL process - orderId={}, productId={}, stockBefore={}, quantity={}, remaining={}",
                order.getId(), product.getProductId(), before, item.getQuantity(), remaining);

        if (remaining < 5) {
            log.warn("PHYSICAL low stock detected - productId={}, remaining={}",
                    product.getProductId(), remaining);
            eventPublisher.publishLowStockAlert(product.getProductId(), remaining);
        }

        if (metadata != null && metadata.containsKey("warehouseLocation")) {
            metadata.put("deliveryEtaDays", "5-10");
        }
    }
}
