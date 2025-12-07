package br.com.loomi.orders.domain.dto;

import br.com.loomi.orders.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO returned after successfully creating an order.
 * Contains the order ID, status, total amount, and creation timestamp.
 */
public class CreateOrderResponse {

    private Long orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Instant createdAt;

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
}
