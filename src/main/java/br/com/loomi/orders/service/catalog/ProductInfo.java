package br.com.loomi.orders.service.catalog;

import br.com.loomi.orders.domain.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contains product catalog information.
 * Holds product details including pricing, availability, and type-specific attributes.
 */
public class ProductInfo {

    private String productId;
    private String name;
    private ProductType productType;
    private BigDecimal price;
    private Integer stock;
    private Integer licenses;
    private LocalDate releaseDate;
    private Integer preOrderSlots;
    private boolean active = true;

    /**
     * Gets the product identifier.
     *
     * @return the product ID
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Sets the product identifier.
     *
     * @param productId the product ID to set
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Gets the product name.
     *
     * @return the product name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the product name.
     *
     * @param name the product name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the product type.
     *
     * @return the product type
     */
    public ProductType getProductType() {
        return productType;
    }

    /**
     * Sets the product type.
     *
     * @param productType the product type to set
     */
    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    /**
     * Gets the product price.
     *
     * @return the price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the product price.
     *
     * @param price the price to set
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Gets the available stock quantity for physical/corporate products.
     *
     * @return the stock quantity
     */
    public Integer getStock() {
        return stock;
    }

    /**
     * Sets the available stock quantity.
     *
     * @param stock the stock quantity to set
     */
    public void setStock(Integer stock) {
        this.stock = stock;
    }

    /**
     * Gets the available licenses for digital products.
     *
     * @return the number of licenses
     */
    public Integer getLicenses() {
        return licenses;
    }

    /**
     * Sets the available licenses for digital products.
     *
     * @param licenses the number of licenses to set
     */
    public void setLicenses(Integer licenses) {
        this.licenses = licenses;
    }

    /**
     * Gets the release date for pre-order products.
     *
     * @return the release date
     */
    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    /**
     * Sets the release date for pre-order products.
     *
     * @param releaseDate the release date to set
     */
    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    /**
     * Gets the available pre-order slots.
     *
     * @return the number of pre-order slots
     */
    public Integer getPreOrderSlots() {
        return preOrderSlots;
    }

    /**
     * Sets the available pre-order slots.
     *
     * @param preOrderSlots the number of pre-order slots to set
     */
    public void setPreOrderSlots(Integer preOrderSlots) {
        this.preOrderSlots = preOrderSlots;
    }

    /**
     * Checks if the product is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the product is active.
     *
     * @param active the active status to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
