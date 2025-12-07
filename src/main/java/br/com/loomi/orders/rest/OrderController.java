package br.com.loomi.orders.rest;

import br.com.loomi.orders.domain.dto.CreateOrderRequest;
import br.com.loomi.orders.domain.dto.CreateOrderResponse;
import br.com.loomi.orders.domain.dto.OrderDetailResponse;
import br.com.loomi.orders.domain.dto.OrderSummaryResponse;
import br.com.loomi.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management operations.
 * Provides endpoints for creating, retrieving, and listing orders.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Constructs the order controller with the required service.
     *
     * @param orderService the order service
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates a new order.
     *
     * @param request the order creation request
     * @return response entity containing the created order details
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves order details by ID.
     *
     * @param orderId the order identifier
     * @return response entity containing order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    /**
     * Lists orders for a specific customer with pagination.
     *
     * @param customerId the customer identifier
     * @param page the page number (zero-based)
     * @param size the page size
     * @return response entity containing paginated order summaries
     */
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> listByCustomer(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId, pageable));
    }
}
