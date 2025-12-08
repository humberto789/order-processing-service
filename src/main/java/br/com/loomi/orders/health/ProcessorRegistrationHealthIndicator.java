package br.com.loomi.orders.health;

import br.com.loomi.orders.domain.enums.ProductType;
import br.com.loomi.orders.service.processing.OrderItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Custom health indicator that verifies all product type processors
 * are registered and available.
 */
@Component
public class ProcessorRegistrationHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorRegistrationHealthIndicator.class);

    private static final String DETAIL_REGISTERED_PROCESSORS = "registeredProcessors";
    private static final String DETAIL_REGISTERED_TYPES = "registeredTypes";
    private static final String DETAIL_MISSING_TYPES = "missingTypes";
    private static final String DETAIL_ERROR = "error";
    private static final String DETAIL_STATUS = "status";

    private static final String STATUS_ALL_OK = "All product types have processors";
    private static final String STATUS_MISSING = "Missing processors for some product types";
    private static final String STATUS_ERROR = "Error verifying processors";

    private static final String LOG_MISSING_PROCESSORS =
            "Health check warning - missing processors for types: {}";
    private static final String LOG_ERROR_VERIFYING =
            "Health check failed - error verifying processors";

    private final List<OrderItemProcessor> processors;

    public ProcessorRegistrationHealthIndicator(List<OrderItemProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public Health health() {
        try {
            Set<ProductType> registeredTypes = EnumSet.noneOf(ProductType.class);
            List<String> processorNames = new ArrayList<>();

            for (OrderItemProcessor processor : processors) {
                String className = processor.getClass().getSimpleName();
                processorNames.add(className);

                if (className.startsWith("Physical")) {
                    registeredTypes.add(ProductType.PHYSICAL);
                } else if (className.startsWith("Subscription")) {
                    registeredTypes.add(ProductType.SUBSCRIPTION);
                } else if (className.startsWith("Digital")) {
                    registeredTypes.add(ProductType.DIGITAL);
                } else if (className.startsWith("PreOrder")) {
                    registeredTypes.add(ProductType.PRE_ORDER);
                } else if (className.startsWith("Corporate")) {
                    registeredTypes.add(ProductType.CORPORATE);
                }
            }

            Set<ProductType> allTypes = EnumSet.allOf(ProductType.class);
            Set<ProductType> missingTypes = EnumSet.copyOf(allTypes);
            missingTypes.removeAll(registeredTypes);

            if (missingTypes.isEmpty()) {
                return Health.up()
                        .withDetail(DETAIL_REGISTERED_PROCESSORS, processorNames)
                        .withDetail(DETAIL_REGISTERED_TYPES, registeredTypes)
                        .withDetail(DETAIL_STATUS, STATUS_ALL_OK)
                        .build();
            } else {
                LOGGER.warn(LOG_MISSING_PROCESSORS, missingTypes);

                return Health.down()
                        .withDetail(DETAIL_REGISTERED_PROCESSORS, processorNames)
                        .withDetail(DETAIL_REGISTERED_TYPES, registeredTypes)
                        .withDetail(DETAIL_MISSING_TYPES, missingTypes)
                        .withDetail(DETAIL_STATUS, STATUS_MISSING)
                        .build();
            }

        } catch (Exception e) {
            LOGGER.error(LOG_ERROR_VERIFYING, e);

            return Health.down()
                    .withDetail(DETAIL_ERROR, e.getMessage())
                    .withDetail(DETAIL_STATUS, STATUS_ERROR)
                    .build();
        }
    }
}
