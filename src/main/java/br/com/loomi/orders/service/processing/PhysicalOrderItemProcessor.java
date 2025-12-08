package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.service.catalog.ProductCatalogService;
import br.com.loomi.orders.service.catalog.ProductInfo;
import br.com.loomi.orders.service.event.OrderEventPublisher;
import br.com.loomi.orders.service.metrics.OrderMetricsService;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PhysicalOrderItemProcessor.class);

    private static final String METADATA_KEY_WAREHOUSE_LOCATION = "warehouseLocation";
    private static final String METADATA_KEY_DELIVERY_ETA_DAYS = "deliveryEtaDays";
    private static final String DELIVERY_ETA_DEFAULT = "5-10";
    private static final String ERROR_CODE_WAREHOUSE_UNAVAILABLE = "WAREHOUSE_UNAVAILABLE";
    private static final String WAREHOUSE_UNAVAILABLE_PREFIX = "UNAVAILABLE";

    private final ProductCatalogService catalogService;
    private final InventoryService inventoryService;
    private final OrderEventPublisher eventPublisher;
    private final OrderMetricsService metricsService;

    public PhysicalOrderItemProcessor(ProductCatalogService catalogService,
                                      InventoryService inventoryService,
                                      OrderEventPublisher eventPublisher,
                                      OrderMetricsService metricsService) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
        this.metricsService = metricsService;
    }

    @Override
    public void process(Order order, OrderItem item, OrderProcessingContext context) {
        ProductInfo product = catalogService.getRequiredProduct(item.getProductId());

        Map<String, Object> metadata = item.getMetadata();
        String warehouseLocation = metadata != null && metadata.get(METADATA_KEY_WAREHOUSE_LOCATION) != null
                ? metadata.get(METADATA_KEY_WAREHOUSE_LOCATION).toString()
                : null;

        if (warehouseLocation != null
                && warehouseLocation.toUpperCase().startsWith(WAREHOUSE_UNAVAILABLE_PREFIX)) {

            LOGGER.warn("WAREHOUSE_UNAVAILABLE - orderId={}, productId={}, warehouseLocation={}",
                    order.getId(), product.getProductId(), warehouseLocation);

            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ERROR_CODE_WAREHOUSE_UNAVAILABLE,
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

        LOGGER.info("PHYSICAL process - orderId={}, productId={}, stockBefore={}, quantity={}, remaining={}",
                order.getId(), product.getProductId(), before, item.getQuantity(), remaining);

        if (remaining < 5) {
            LOGGER.warn("PHYSICAL low stock detected - productId={}, remaining={}",
                    product.getProductId(), remaining);
            eventPublisher.publishLowStockAlert(product.getProductId(), remaining);
            metricsService.recordLowStockAlert(product.getProductId());
        }

        if (metadata != null && metadata.containsKey(METADATA_KEY_WAREHOUSE_LOCATION)) {
            metadata.put(METADATA_KEY_DELIVERY_ETA_DAYS, DELIVERY_ETA_DEFAULT);
        }
    }
}
