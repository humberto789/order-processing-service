package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.service.catalog.ProductCatalogService;
import br.com.loomi.orders.service.catalog.ProductInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PreOrderService {

    private final ProductCatalogService catalogService;
    private final Map<String, Integer> reservedSlots = new ConcurrentHashMap<>();

    public PreOrderService(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public synchronized void validateAndReserve(String productId, int quantity) {
        ProductInfo info = catalogService.getRequiredProduct(productId);

        LocalDate release = info.getReleaseDate();
        if (release == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.INVALID_RELEASE_DATE.name(),
                    "Release date not configured for " + productId
            );
        }

        if (!release.isAfter(LocalDate.now())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.RELEASE_DATE_PASSED.name(),
                    "Release date already passed for " + productId
            );
        }

        Integer totalSlots = info.getPreOrderSlots();
        if (totalSlots == null || totalSlots <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.PRE_ORDER_SOLD_OUT.name(),
                    "No pre-order slots available for " + productId
            );
        }

        int reserved = reservedSlots.getOrDefault(productId, 0);
        if (reserved + quantity > totalSlots) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.PRE_ORDER_SOLD_OUT.name(),
                    "Pre-order slots exceeded for " + productId
            );
        }

        reservedSlots.put(productId, reserved + quantity);
    }
}
