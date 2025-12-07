package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.enums.OrderFailureReason;

import java.math.BigDecimal;

/**
 * Context object that holds state during order processing.
 * Used to track processing flags, failures, and approval requirements.
 */
public class OrderProcessingContext {

    private boolean highValue;
    private boolean fraudAlert;
    private boolean pendingApproval;
    private OrderFailureReason failureReason;
    private String failureMessage;
    private BigDecimal totalAmount;

    public OrderProcessingContext() {
    }

    /**
     * Checks if the order is high value.
     *
     * @return true if high value, false otherwise
     */
    public boolean isHighValue() {
        return highValue;
    }

    /**
     * Sets the high value flag.
     *
     * @param highValue true if high value
     */
    public void setHighValue(boolean highValue) {
        this.highValue = highValue;
    }

    /**
     * Checks if a fraud alert has been triggered.
     *
     * @return true if fraud alert is active, false otherwise
     */
    public boolean isFraudAlert() {
        return fraudAlert;
    }

    /**
     * Sets the fraud alert flag.
     *
     * @param fraudAlert true if fraud alert should be active
     */
    public void setFraudAlert(boolean fraudAlert) {
        this.fraudAlert = fraudAlert;
    }

    /**
     * Checks if the order is pending manual approval.
     *
     * @return true if pending approval, false otherwise
     */
    public boolean isPendingApproval() {
        return pendingApproval;
    }

    /**
     * Sets the pending approval flag.
     *
     * @param pendingApproval true if approval is required
     */
    public void setPendingApproval(boolean pendingApproval) {
        this.pendingApproval = pendingApproval;
    }

    /**
     * Gets the order failure reason.
     *
     * @return the failure reason
     */
    public OrderFailureReason getFailureReason() {
        return failureReason;
    }

    /**
     * Sets the order failure reason.
     *
     * @param failureReason the failure reason to set
     */
    public void setFailureReason(OrderFailureReason failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Gets the failure message.
     *
     * @return the failure message
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * Sets the failure message.
     *
     * @param failureMessage the failure message to set
     */
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
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
}
