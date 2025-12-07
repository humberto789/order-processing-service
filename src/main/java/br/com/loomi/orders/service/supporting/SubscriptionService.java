package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubscriptionService {

    private static final int MAX_ACTIVE_SUBSCRIPTIONS = 5;

    private final Map<String, Set<String>> activeSubscriptionsByCustomer = new ConcurrentHashMap<>();

    public synchronized void validateAndActivate(String customerId, String productId) {
        Set<String> active = activeSubscriptionsByCustomer
                .computeIfAbsent(customerId, k -> new HashSet<>());

        if (active.contains(productId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.DUPLICATE_ACTIVE_SUBSCRIPTION.name(),
                    "Customer %s already has active subscription for %s"
                            .formatted(customerId, productId)
            );
        }

        if (active.size() >= MAX_ACTIVE_SUBSCRIPTIONS) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.SUBSCRIPTION_LIMIT_EXCEEDED.name(),
                    "Customer %s reached the maximum number of active subscriptions (%d)"
                            .formatted(customerId, MAX_ACTIVE_SUBSCRIPTIONS)
            );
        }

        boolean isEnterprise = productId.startsWith("SUB-ENTERPRISE");
        boolean isBasicOrPremium = productId.startsWith("SUB-BASIC") || productId.startsWith("SUB-PREMIUM");

        boolean hasEnterprise = active.stream()
                .anyMatch(p -> p.startsWith("SUB-ENTERPRISE"));

        boolean hasBasicOrPremium = active.stream()
                .anyMatch(p -> p.startsWith("SUB-BASIC") || p.startsWith("SUB-PREMIUM"));

        if ((isEnterprise && hasBasicOrPremium) || (isBasicOrPremium && hasEnterprise)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.INCOMPATIBLE_SUBSCRIPTIONS.name(),
                    "Incompatible subscription plans for customer %s".formatted(customerId)
            );
        }

        active.add(productId);
    }
}
