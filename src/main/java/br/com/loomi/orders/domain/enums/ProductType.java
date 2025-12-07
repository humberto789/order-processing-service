package br.com.loomi.orders.domain.enums;

/**
 * Represents the different types of products available in the system.
 * Each product type has specific processing requirements and validations.
 */
public enum ProductType {
    PHYSICAL,
    SUBSCRIPTION,
    DIGITAL,
    PRE_ORDER,
    CORPORATE
}
