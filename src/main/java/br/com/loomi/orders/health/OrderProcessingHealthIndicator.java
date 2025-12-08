package br.com.loomi.orders.health;

import br.com.loomi.orders.persistence.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator that verifies database connectivity
 * and ability to query orders.
 */
@Component
public class OrderProcessingHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingHealthIndicator.class);

    private final OrderRepository orderRepository;

    public OrderProcessingHealthIndicator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Health health() {
        try {
            long orderCount = orderRepository.count();

            return Health.up()
                    .withDetail("totalOrders", orderCount)
                    .withDetail("status", "Database accessible")
                    .build();

        } catch (Exception e) {
            LOGGER.error("Health check failed - database not accessible", e);

            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Database not accessible")
                    .build();
        }
    }
}
