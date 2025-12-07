package br.com.loomi.orders.domain.dto;

import br.com.loomi.orders.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response object containing a summary of an order.
 * Provides basic order information without detailed item listings.
 */
public class OrderSummaryResponse {

    private Long orderId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
