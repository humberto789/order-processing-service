package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    private final Map<String, Integer> stockByProduct = new ConcurrentHashMap<>();

    public synchronized int getStock(String productId) {
        return stockByProduct.getOrDefault(productId, 0);
    }

    public synchronized void initStockIfAbsent(String productId, int stock) {
        stockByProduct.putIfAbsent(productId, stock);
    }

    public synchronized void reserve(String productId, int quantity) {
        int current = stockByProduct.getOrDefault(productId, 0);

        if (current < quantity) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    OrderFailureReason.OUT_OF_STOCK.name(),
                    "Not enough stock for " + productId
            );
        }

        stockByProduct.put(productId, current - quantity);
    }
}
