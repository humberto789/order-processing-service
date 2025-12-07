package br.com.loomi.orders.domain.enums;

/**
 * Represents the various reasons why an order might fail or require special handling.
 * Reasons are categorized by product type and global validation failures.
 */
public enum OrderFailureReason {

    // PHYSICAL
    OUT_OF_STOCK,
    WAREHOUSE_UNAVAILABLE,

    // SUBSCRIPTION
    SUBSCRIPTION_LIMIT_EXCEEDED,
    DUPLICATE_ACTIVE_SUBSCRIPTION,
    INCOMPATIBLE_SUBSCRIPTIONS,

    // DIGITAL
    LICENSE_UNAVAILABLE,
    ALREADY_OWNED,
    DISTRIBUTION_RIGHTS_EXPIRED,

    // PRE_ORDER
    PRE_ORDER_SOLD_OUT,
    RELEASE_DATE_PASSED,
    INVALID_RELEASE_DATE,

    // CORPORATE
    CREDIT_LIMIT_EXCEEDED,
    INVALID_CORPORATE_DATA,
    PENDING_MANUAL_APPROVAL,

    // GLOBAL
    PAYMENT_FAILED,
    FRAUD_ALERT,
    INVALID_REQUEST
}
