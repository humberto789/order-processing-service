package br.com.loomi.orders.service.catalog;

import br.com.loomi.orders.domain.enums.ProductType;
import br.com.loomi.orders.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the product catalog service.
 * Stores product information in a concurrent hash map for thread-safe access.
 */
@Service
public class InMemoryProductCatalogService implements ProductCatalogService {

    private final Map<String, ProductInfo> products = new ConcurrentHashMap<>();

    public InMemoryProductCatalogService() {
        // Físicos
        addPhysical("BOOK-CC-001", "Clean Code", 89.90, 150);
        addPhysical("LAPTOP-PRO-2024", "Laptop Pro", 5499.00, 8);
        addPhysical("LAPTOP-MBP-M3-001", "MacBook Pro M3", 12999.00, 25);

        // Assinaturas
        addSubscription("SUB-PREMIUM-001", "Premium Monthly", 49.90);
        addSubscription("SUB-BASIC-001", "Basic Monthly", 19.90);
        addSubscription("SUB-BASIC-002", "Basic 2 Monthly", 299.00);
        addSubscription("SUB-BASIC-003", "Basic 3 Monthly", 159.00);
        addSubscription("SUB-BASIC-004", "Basic 4 Monthly", 159.00);
        addSubscription("SUB-BASIC-005", "Basic 3 Monthly", 159.00);
        addSubscription("SUB-BASIC-006", "Basic 5 Monthly", 159.00);
        addSubscription("SUB-ENTERPRISE-001", "Enterprise Plan", 299.00);
        addSubscription("SUB-ADOBE-CC-001", "Adobe Creative Cloud", 159.00);

        // Digitais
        addDigital("EBOOK-JAVA-001", "Effective Java", 39.90, 1000);
        addDigital("EBOOK-DDD-001", "Domain-Driven Design", 59.90, 500);
        addDigital("EBOOK-SWIFT-001", "Swift Programming", 49.90, 800);
        addDigital("COURSE-KAFKA-001", "Kafka Mastery", 299.00, 500);

        // Pré-venda
        addPreOrder("GAME-2025-001", "Epic Game 2025", 249.90, LocalDate.now().plusMonths(3), 1000);
        addPreOrder("PRE-PS6-001", "PlayStation 6", 4999.00, LocalDate.of(2025,11,15), 500);
        addPreOrder("PRE-IPHONE16-001", "iPhone 16 Pro", 7999.00, LocalDate.of(2025,9,20), 2000);

        // Corporativo
        addCorporate("CORP-LICENSE-ENT", "Enterprise License", 15000.00);
        addPhysicalCorporate("CORP-CHAIR-ERG-001", "Ergonomic Chair Bulk", 899.00, 500);
    }

    private void addPhysical(String id, String name, double price, int stock) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setName(name);
        p.setProductType(ProductType.PHYSICAL);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStock(stock);
        products.put(id, p);
    }

    private void addSubscription(String id, String name, double price) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setName(name);
        p.setProductType(ProductType.SUBSCRIPTION);
        p.setPrice(BigDecimal.valueOf(price));
        products.put(id, p);
    }

    private void addDigital(String id, String name, double price, int licenses) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setName(name);
        p.setProductType(ProductType.DIGITAL);
        p.setPrice(BigDecimal.valueOf(price));
        p.setLicenses(licenses);
        products.put(id, p);
    }

    private void addPreOrder(String id, String name, double price, LocalDate release, int slots) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setName(name);
        p.setProductType(ProductType.PRE_ORDER);
        p.setPrice(BigDecimal.valueOf(price));
        p.setReleaseDate(release);
        p.setPreOrderSlots(slots);
        products.put(id, p);
    }

    private void addCorporate(String id, String name, double price) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setName(name);
        p.setProductType(ProductType.CORPORATE);
        p.setPrice(BigDecimal.valueOf(price));
        products.put(id, p);
    }

    private void addPhysicalCorporate(String id, String name, double price, int stock) {
        ProductInfo p = new ProductInfo();
        p.setProductId(id);
        p.setName(name);
        p.setProductType(ProductType.CORPORATE);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStock(stock);
        products.put(id, p);
    }

    /**
     * Retrieves product information by ID.
     *
     * @param productId the product identifier
     * @return the product information
     */
    @Override
    public ProductInfo getRequiredProduct(String productId) {
        ProductInfo product = products.get(productId);
        if (product == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "OUT_OF_STOCK",
                    "Product %s not found".formatted(productId));
        }
        if (!product.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "PRODUCT_INACTIVE",
                    "Product %s is not available".formatted(productId));
        }
        return product;
    }
}
