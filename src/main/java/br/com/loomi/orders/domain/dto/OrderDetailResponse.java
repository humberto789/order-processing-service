package br.com.loomi.orders.domain.dto;

import br.com.loomi.orders.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO containing detailed information about an order.
 * Includes order data, customer information, and all order items.
 */
public class OrderDetailResponse {

    /**
     * Represents a single item within an order.
     *
     * @param id the item identifier
     * @param productId the product identifier
     * @param productType the product type
     * @param quantity the quantity ordered
     * @param unitPrice the unit price
     * @param totalPrice the total price for this item
     * @param metadata additional metadata for the item
     */
    public record Item(
            Long id,
            String productId,
            String productType,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            Map<String, Object> metadata
    ) {}

    private Long orderId;
    private String customerId;
    private List<Item> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public OrderDetailResponse() {
    }

    /**
     * Gets the unique order identifier.
     *
     * @return the order ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * Sets the unique order identifier.
     *
     * @param orderId the order ID to set
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets the customer identifier.
     *
     * @return the customer ID
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer identifier.
     *
     * @param customerId the customer ID to set
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the list of order items.
     *
     * @return the order items
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Sets the list of order items.
     *
     * @param items the order items to set
     */
    public void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * Gets the total order amount.
     *
     * @return the total amount
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * Sets the total order amount.
     *
     * @param totalAmount the total amount to set
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * Gets the current order status.
     *
     * @return the order status
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Sets the current order status.
     *
     * @param status the order status to set
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * Gets the order creation timestamp.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the order creation timestamp.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the order last update timestamp.
     *
     * @return the update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the order last update timestamp.
     *
     * @param updatedAt the update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
