package br.com.loomi.orders.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Request DTO for creating an order item.
 * Contains product information, quantity, and optional metadata.
 */
public class CreateOrderItemRequest {

    @NotBlank
    private String productId;

    @NotNull
    @Positive
    private Integer quantity;

    private Map<String, Object> metadata;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
