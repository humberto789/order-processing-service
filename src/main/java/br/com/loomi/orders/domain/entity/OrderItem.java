package br.com.loomi.orders.domain.entity;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.domain.enums.ProductType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents an individual item within an order.
 * Each order item contains product information, pricing, quantity, and optional metadata.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 50)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductType productType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private OrderFailureReason failureReason;

    @Column(length = 255)
    private String failureMessage;

    /**
     * Default constructor for JPA.
     */
    public OrderItem() {
    }

    /**
     * Constructs an OrderItem with all fields.
     *
     * @param id the unique identifier
     * @param order the parent order
     * @param productId the product identifier
     * @param productType the type of product
     * @param quantity the quantity ordered
     * @param unitPrice the price per unit
     * @param totalPrice the total price for this item
     * @param metadata additional metadata for this item
     * @param failureReason the reason for failure if applicable
     * @param failureMessage the failure message if applicable
     */
    @SuppressWarnings("java:S107")
    public OrderItem(Long id, Order order, String productId, ProductType productType,
                     Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice,
                     Map<String, Object> metadata, OrderFailureReason failureReason,
                     String failureMessage) {
        this.id = id;
        this.order = order;
        this.productId = productId;
        this.productType = productType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.metadata = metadata;
        this.failureReason = failureReason;
        this.failureMessage = failureMessage;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
}
