package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage corporate credit limits for customers.
 */
@Service
public class CorporateCreditService {

    private static final BigDecimal CREDIT_LIMIT = BigDecimal.valueOf(100_000);

    private final Map<String, BigDecimal> usedCreditByCustomer = new ConcurrentHashMap<>();

    /**
     * Validates if the customer has enough credit and reserves the specified amount.
     *
     * @param customerId the ID of the customer
     * @param amount     the amount to reserve
     * @throws BusinessException if the credit limit is exceeded
     */
    public synchronized void validateAndReserve(String customerId, BigDecimal amount) {
        BigDecimal used = usedCreditByCustomer.getOrDefault(customerId, BigDecimal.ZERO);

        if (used.add(amount).compareTo(CREDIT_LIMIT) > 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.CREDIT_LIMIT_EXCEEDED.name(),
                    "Credit limit exceeded for customer " + customerId
            );
        }

        usedCreditByCustomer.put(customerId, used.add(amount));
    }
}
