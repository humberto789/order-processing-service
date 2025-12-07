package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.domain.enums.OrderFailureReason;
import br.com.loomi.orders.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory inventory service to manage product stock levels.
 */
@Service
public class InventoryService {

    private final Map<String, Integer> stockByProduct = new ConcurrentHashMap<>();

    /**
     * Gets the current stock level for a given product.
     *
     * @param productId the ID of the product
     * @return the current stock level
     */
    public synchronized int getStock(String productId) {
        return stockByProduct.getOrDefault(productId, 0);
    }

    /**
     * Initializes the stock level for a product if it is not already set.
     *
     * @param productId the ID of the product
     * @param stock     the initial stock level
     */
    public synchronized void initStockIfAbsent(String productId, int stock) {
        stockByProduct.putIfAbsent(productId, stock);
    }

    /**
     * Reserves a quantity of a product, reducing its stock level.
     *
     * @param productId the ID of the product
     * @param quantity  the quantity to reserve
     * @throws BusinessException if there is not enough stock
     */
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
