package br.com.loomi.orders.persistence;

import br.com.loomi.orders.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for Order entity persistence.
 * Provides database access methods for order management.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds orders for a specific customer ordered by creation date descending.
     *
     * @param customerId the customer identifier
     * @param pageable pagination parameters
     * @return page of orders
     */
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
}
