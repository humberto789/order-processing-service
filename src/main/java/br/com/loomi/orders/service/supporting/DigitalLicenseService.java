package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.service.catalog.ProductCatalogService;
import br.com.loomi.orders.service.catalog.ProductInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing digital licenses for products.
 */
@Service
public class DigitalLicenseService {

    private final ProductCatalogService catalogService;
    private final Map<String, Integer> remainingLicenses = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> ownedDigitalByCustomer = new ConcurrentHashMap<>();

    /**
     * Constructs a new DigitalLicenseService with the given ProductCatalogService.
     *
     * @param catalogService the product catalog service
     */
    public DigitalLicenseService(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Allocates digital licenses to a customer for a specific product.
     *
     * @param customerId the ID of the customer
     * @param productId  the ID of the product
     * @param quantity   the number of licenses to allocate
     * @throws BusinessException if the customer already owns the product or if there are not enough licenses available
     */
    public synchronized void allocateLicense(String customerId, String productId, int quantity) {
        Set<String> owned = ownedDigitalByCustomer.computeIfAbsent(customerId, k -> new HashSet<>());

        if (owned.contains(productId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.ALREADY_OWNED.name(),
                    "Customer already owns digital product " + productId
            );
        }

        int remaining = remainingLicenses.computeIfAbsent(productId, p -> {
            ProductInfo info = catalogService.getRequiredProduct(p);
            return info.getLicenses() != null ? info.getLicenses() : 0;
        });

        if (remaining < quantity) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.LICENSE_UNAVAILABLE.name(),
                    "Not enough licenses available for " + productId
            );
        }

        remainingLicenses.put(productId, remaining - quantity);
        owned.add(productId);
    }
}
