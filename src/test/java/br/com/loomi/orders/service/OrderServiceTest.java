package br.com.loomi.orders.service;

import br.com.loomi.orders.domain.dto.CreateOrderItemRequest;
import br.com.loomi.orders.domain.dto.CreateOrderRequest;
import br.com.loomi.orders.domain.dto.CreateOrderResponse;
import br.com.loomi.orders.domain.entity.Order;
import br.com.loomi.orders.domain.entity.OrderItem;
import br.com.loomi.orders.domain.enums.OrderStatus;
import br.com.loomi.orders.domain.enums.ProductType;
import br.com.loomi.orders.exception.BusinessException;
import br.com.loomi.orders.persistence.OrderRepository;
import br.com.loomi.orders.service.catalog.ProductCatalogService;
import br.com.loomi.orders.service.catalog.ProductInfo;
import br.com.loomi.orders.service.event.OrderEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductCatalogService catalogService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private ProductInfo createProduct(String id, String name, ProductType type, double price) {
        ProductInfo product = new ProductInfo();
        product.setProductId(id);
        product.setName(name);
        product.setProductType(type);
        product.setPrice(BigDecimal.valueOf(price));
        product.setActive(true);
        return product;
    }

    @Nested
    @DisplayName("Order Creation Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully with valid request")
        void shouldCreateOrderSuccessfully() {
            // Arrange
            ProductInfo product = createProduct("BOOK-001", "Test Book", ProductType.PHYSICAL, 50.00);
            when(catalogService.getRequiredProduct("BOOK-001")).thenReturn(product);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
            itemRequest.setProductId("BOOK-001");
            itemRequest.setQuantity(2);

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(List.of(itemRequest));

            // Act
            CreateOrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getOrderId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");

            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getCustomerId()).isEqualTo("customer-123");
            assertThat(savedOrder.getItems()).hasSize(1);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("100.00");

            verify(eventPublisher).publishOrderCreated(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception for empty items list")
        void shouldThrowExceptionForEmptyItems() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(List.of());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("EMPTY_ITEMS");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });

            verify(orderRepository, never()).save(any());
            verify(eventPublisher, never()).publishOrderCreated(any());
        }

        @Test
        @DisplayName("Should throw exception for null items list")
        void shouldThrowExceptionForNullItems() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(null);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should propagate exception when product not found")
        void shouldPropagateExceptionWhenProductNotFound() {
            // Arrange
            when(catalogService.getRequiredProduct("NON-EXISTENT"))
                    .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Product not found"));

            CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
            itemRequest.setProductId("NON-EXISTENT");
            itemRequest.setQuantity(1);

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(List.of(itemRequest));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should calculate total for multiple items correctly")
        void shouldCalculateTotalForMultipleItems() {
            // Arrange
            ProductInfo product1 = createProduct("BOOK-001", "Book 1", ProductType.PHYSICAL, 30.00);
            ProductInfo product2 = createProduct("BOOK-002", "Book 2", ProductType.PHYSICAL, 50.00);

            when(catalogService.getRequiredProduct("BOOK-001")).thenReturn(product1);
            when(catalogService.getRequiredProduct("BOOK-002")).thenReturn(product2);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            CreateOrderItemRequest item1 = new CreateOrderItemRequest();
            item1.setProductId("BOOK-001");
            item1.setQuantity(2); // 60.00

            CreateOrderItemRequest item2 = new CreateOrderItemRequest();
            item2.setProductId("BOOK-002");
            item2.setQuantity(3); // 150.00

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(List.of(item1, item2));

            // Act
            CreateOrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.getTotalAmount()).isEqualByComparingTo("210.00");
        }

        @Test
        @DisplayName("Should store correct product type from catalog")
        void shouldStoreCorrectProductType() {
            // Arrange
            ProductInfo product = createProduct("SUB-001", "Subscription", ProductType.SUBSCRIPTION, 29.90);
            when(catalogService.getRequiredProduct("SUB-001")).thenReturn(product);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
            itemRequest.setProductId("SUB-001");
            itemRequest.setQuantity(1);

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(List.of(itemRequest));

            // Act
            orderService.createOrder(request);

            // Assert
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getItems().get(0).getProductType()).isEqualTo(ProductType.SUBSCRIPTION);
        }

        @Test
        @DisplayName("Should store metadata in order item")
        void shouldStoreMetadataInOrderItem() {
            // Arrange
            ProductInfo product = createProduct("BOOK-001", "Book", ProductType.PHYSICAL, 50.00);
            when(catalogService.getRequiredProduct("BOOK-001")).thenReturn(product);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            Map<String, Object> metadata = Map.of("warehouseLocation", "SP", "giftWrap", true);

            CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
            itemRequest.setProductId("BOOK-001");
            itemRequest.setQuantity(1);
            itemRequest.setMetadata(metadata);

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId("customer-123");
            request.setItems(List.of(itemRequest));

            // Act
            orderService.createOrder(request);

            // Assert
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getItems().get(0).getMetadata())
                    .containsEntry("warehouseLocation", "SP")
                    .containsEntry("giftWrap", true);
        }
    }

    @Nested
    @DisplayName("Order Retrieval Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should retrieve order by ID successfully")
        void shouldRetrieveOrderById() {
            // Arrange
            Order order = new Order();
            order.setId(1L);
            order.setCustomerId("customer-123");
            order.setTotalAmount(BigDecimal.valueOf(100));
            order.setStatus(OrderStatus.PROCESSED);

            OrderItem item = new OrderItem();
            item.setProductId("BOOK-001");
            item.setProductType(ProductType.PHYSICAL);
            item.setQuantity(2);
            item.setUnitPrice(BigDecimal.valueOf(50));
            item.setTotalPrice(BigDecimal.valueOf(100));
            order.addItem(item);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // Act
            var response = orderService.getOrder(1L);

            // Assert
            assertThat(response.getOrderId()).isEqualTo(1L);
            assertThat(response.getCustomerId()).isEqualTo("customer-123");
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSED);
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrder(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("ORDER_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}
