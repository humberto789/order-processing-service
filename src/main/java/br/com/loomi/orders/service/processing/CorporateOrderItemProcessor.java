package br.com.loomi.orders.service.processing;

import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.service.supporting.CorporateCreditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Processor for corporate product order items.
 * Handles CNPJ validation, payment terms, bulk discounts, and credit validation.
 */
@Component
public class CorporateOrderItemProcessor implements OrderItemProcessor {

    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");

    private final CorporateCreditService corporateCreditService;

    /**
     * Constructs the processor with the required service.
     *
     * @param corporateCreditService the corporate credit service
     */
    public CorporateOrderItemProcessor(CorporateCreditService corporateCreditService) {
        this.corporateCreditService = corporateCreditService;
    }

    /**
     * Processes a corporate product order item.
     * Validates CNPJ, applies payment terms and bulk discounts, validates credit.
     *
     * @param order the parent order
     * @param item the order item to process
     * @param context the processing context
     */
    @Override
    public void process(Order order, OrderItem item, OrderProcessingContext context) {
        Map<String, Object> metadata = item.getMetadata();

        if (metadata == null || !metadata.containsKey("cnpj")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "INVALID_CORPORATE_DATA",
                    "CNPJ is required for corporate orders");
        }

        String cnpj = metadata.get("cnpj").toString();
        if (!CNPJ_PATTERN.matcher(cnpj).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "INVALID_CORPORATE_DATA",
                    "Invalid CNPJ format: " + cnpj);
        }

        if (metadata.containsKey("paymentTerms")) {
            String terms = metadata.get("paymentTerms").toString();
            int days = switch (terms) {
                case "NET_30" -> 30;
                case "NET_60" -> 60;
                case "NET_90" -> 90;
                default -> 30;
            };
            metadata.put("paymentDueDays", days);
        }

        if (item.getQuantity() != null && item.getQuantity() > 100) {
            BigDecimal originalTotal = item.getTotalPrice();
            BigDecimal discount = originalTotal.multiply(BigDecimal.valueOf(0.15));
            BigDecimal newTotal = originalTotal.subtract(discount);
            item.setTotalPrice(newTotal);
            order.setTotalAmount(order.getTotalAmount().subtract(discount));
        }

        corporateCreditService.validateAndReserve(order.getCustomerId(), item.getTotalPrice());

        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(50_000)) > 0) {
            context.setPendingApproval(true);
            context.setFailureReason(OrderFailureReason.PENDING_MANUAL_APPROVAL);
            context.setFailureMessage("Corporate orders above 50,000 require manual approval");
        }
    }
}
