package br.com.loomi.orders.service.catalog;

/**
 * Service interface for product catalog operations.
 * Provides access to product information.
 */
public interface ProductCatalogService {

    /**
     * Retrieves product information by ID.
     * Throws an exception if the product is not found or inactive.
     *
     * @param productId the product identifier
     * @return the product information
     */
    ProductInfo getRequiredProduct(String productId);
}
