package br.com.loomi.orders.domain.entity;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.domain.enums.OrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an order entity in the system.
 * This entity manages the order lifecycle, including items, status, and failure tracking.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String customerId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private OrderFailureReason failureReason;

    @Column(length = 255)
    private String failureMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public Order() {
    }

    /**
     * Constructs an Order with all fields.
     *
     * @param id the unique identifier
     * @param customerId the customer identifier
     * @param items the list of order items
     * @param totalAmount the total order amount
     * @param status the current order status
     * @param failureReason the reason for failure if applicable
     * @param failureMessage the failure message if applicable
     * @param createdAt the creation timestamp
     * @param updatedAt the last update timestamp
     */
    public Order(Long id, String customerId, List<OrderItem> items, BigDecimal totalAmount,
                 OrderStatus status, OrderFailureReason failureReason, String failureMessage,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.status = status;
        this.failureReason = failureReason;
        this.failureMessage = failureMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
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

    public OrderFailureReason getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(OrderFailureReason failureReason) {
        this.failureReason = failureReason;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Adds an item to this order and establishes the bidirectional relationship.
     *
     * @param item the order item to add
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    /**
     * Marks this order as pending.
     */
    public void markPending() {
        this.status = OrderStatus.PENDING;
    }

    /**
     * Marks this order as processed and clears any failure information.
     */
    public void markProcessed() {
        this.status = OrderStatus.PROCESSED;
        this.failureReason = null;
        this.failureMessage = null;
    }

    /**
     * Marks this order as failed with a reason and message.
     *
     * @param reason the failure reason
     * @param message the failure message
     */
    public void markFailed(OrderFailureReason reason, String message) {
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
        this.failureMessage = message;
    }

    /**
     * Marks this order as pending approval with a message.
     *
     * @param message the message explaining why approval is needed
     */
    public void markPendingApproval(String message) {
        this.status = OrderStatus.PENDING_APPROVAL;
        this.failureReason = OrderFailureReason.PENDING_MANUAL_APPROVAL;
        this.failureMessage = message;
    }

    /**
     * JPA lifecycle callback executed before persisting the entity.
     * Sets the creation and update timestamps.
     */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA lifecycle callback executed before updating the entity.
     * Updates the update timestamp.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
