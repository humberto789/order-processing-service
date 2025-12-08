package br.com.loomi.orders.service.metrics;

import br.com.loomi.orders.domain.enums.OrderStatus;
import br.com.loomi.orders.domain.enums.ProductType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for recording custom business metrics.
 * All metrics are exposed via /actuator/prometheus endpoint.
 */
@Service
public class OrderMetricsService {

    private final Counter ordersCreatedCounter;
    private final DistributionSummary orderAmountSummary;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs the metrics service and initializes counters and summaries.
     *
     * @param meterRegistry the Micrometer meter registry
     */
    public OrderMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .register(meterRegistry);

        this.orderAmountSummary = DistributionSummary.builder("orders.amount")
                .description("Distribution of order amounts")
                .baseUnit("BRL")
                .register(meterRegistry);
    }

    /**
     * Record that an order was created.
     * Increments: orders.created.total
     * Records: orders.amount distribution
     *
     * @param totalAmount total amount of the created order
     */
    public void recordOrderCreated(BigDecimal totalAmount) {
        ordersCreatedCounter.increment();
        orderAmountSummary.record(totalAmount.doubleValue());
    }

    /**
     * Record that an order was processed with a specific status.
     * Increments: orders.processed.total{status="PROCESSED|FAILED|PENDING_APPROVAL"}
     *
     * @param status final status of the processed order
     */
    public void recordOrderProcessed(OrderStatus status) {
        Counter.builder("orders.processed.total")
                .description("Total orders processed by final status")
                .tag("status", status.name())
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record that an order processing failed.
     * Increments: orders.failed.total{reason="OUT_OF_STOCK|INSUFFICIENT_CREDIT|..."}
     *
     * @param reason reason why the order processing failed
     */
    public void recordOrderFailed(String reason) {
        Counter.builder("orders.failed.total")
                .description("Total orders failed by reason")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record that an order item was processed by product type.
     * Increments: orders.items.processed.total{product_type="PHYSICAL|DIGITAL|..."}
     *
     * @param productType type of the processed product
     */
    public void recordProductTypeProcessed(ProductType productType) {
        Counter.builder("orders.items.processed.total")
                .description("Total order items processed by product type")
                .tag("product_type", productType.name())
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a high-value order (> configured threshold).
     * Increments: orders.high_value.total
     * Records: orders.high_value.amount distribution
     *
     * @param amount total amount of the high-value order
     */
    public void recordHighValueOrder(BigDecimal amount) {
        Counter.builder("orders.high_value.total")
                .description("Total high-value orders")
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("orders.high_value.amount")
                .description("Distribution of high-value order amounts")
                .baseUnit("BRL")
                .register(meterRegistry)
                .record(amount.doubleValue());
    }

    /**
     * Record a fraud alert.
     * Increments: orders.fraud_alert.total
     * Records: orders.fraud_alert.amount distribution
     *
     * @param amount total amount of the order that triggered the fraud alert
     */
    public void recordFraudAlert(BigDecimal amount) {
        Counter.builder("orders.fraud_alert.total")
                .description("Total fraud alerts triggered")
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("orders.fraud_alert.amount")
                .description("Distribution of amounts that triggered fraud alerts")
                .baseUnit("BRL")
                .register(meterRegistry)
                .record(amount.doubleValue());
    }

    /**
     * Record a low stock alert for a specific product.
     * Increments: inventory.low_stock.total{product_id="..."}
     *
     * @param productId identifier of the product with low stock
     */
    public void recordLowStockAlert(String productId) {
        Counter.builder("inventory.low_stock.total")
                .description("Total low stock alerts by product")
                .tag("product_id", productId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Start timing an order processing operation.
     * Use with recordOrderProcessingTime() to record the duration.
     *
     * @return a Timer.Sample used to measure the processing duration
     */
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record the time taken to process an order.
     * Records: orders.processing.duration{status="PROCESSED|FAILED|PENDING_APPROVAL"}
     *
     * @param sample      the Timer.Sample started before processing
     * @param finalStatus final status of the processed order
     */
    public void recordOrderProcessingTime(Timer.Sample sample, OrderStatus finalStatus) {
        sample.stop(Timer.builder("orders.processing.duration")
                .description("Time taken to process orders")
                .tag("status", finalStatus.name())
                .register(meterRegistry));
    }
}
