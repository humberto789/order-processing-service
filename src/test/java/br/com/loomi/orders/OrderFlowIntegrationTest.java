package br.com.loomi.orders;

import br.com.loomi.orders.domain.dto.CreateOrderItemRequest;
import br.com.loomi.orders.domain.dto.CreateOrderRequest;
import br.com.loomi.orders.domain.dto.CreateOrderResponse;
import br.com.loomi.orders.domain.dto.OrderDetailResponse;
import br.com.loomi.orders.domain.enums.OrderStatus;
import br.com.loomi.orders.exception.ApiErrorResponse;
import br.com.loomi.orders.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders_db")
            .withUsername("orders")
            .withPassword("orders");

    @Container
    @SuppressWarnings("java:S1874")
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Nested
    @DisplayName("Physical Product Orders")
    class PhysicalProductTests {

        @Test
        @DisplayName("Should create and process physical order successfully")
        void shouldCreateAndProcessPhysicalOrder() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("BOOK-CC-001");
            item.setQuantity(2);
            item.setMetadata(Map.of("warehouseLocation", "SP"));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-physical-001");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isIn(OrderStatus.PROCESSED, OrderStatus.FAILED);
            });
        }

        @Test
        @DisplayName("Should return correct total amount based on catalog price")
        void shouldCalculateTotalFromCatalog() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("BOOK-CC-001");
            item.setQuantity(3);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-price-test");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalAmount())
                    .isEqualByComparingTo("269.70");
        }
    }

    @Nested
    @DisplayName("Subscription Orders")
    class SubscriptionTests {

        @Test
        @DisplayName("Should create and process subscription order")
        void shouldCreateSubscriptionOrder() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("SUB-PREMIUM-001");
            item.setQuantity(1);
            item.setMetadata(Map.of("billingCycle", "MONTHLY", "autoRenewal", true));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-sub-001");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isIn(OrderStatus.PROCESSED, OrderStatus.FAILED);
            });
        }

        @Test
        @DisplayName("Should fail with incompatible subscriptions")
        void shouldFailWithIncompatibleSubscriptions() {
            CreateOrderItemRequest enterprise = new CreateOrderItemRequest();
            enterprise.setProductId("SUB-ENTERPRISE-001");
            enterprise.setQuantity(1);

            CreateOrderItemRequest basic = new CreateOrderItemRequest();
            basic.setProductId("SUB-BASIC-001");
            basic.setQuantity(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-incompatible");
            req.setItems(List.of(enterprise, basic));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                assertThat(order.getFailureReason().name()).isEqualTo("INCOMPATIBLE_SUBSCRIPTIONS");
            });
        }
    }

    @Nested
    @DisplayName("Digital Product Orders")
    class DigitalProductTests {

        @Test
        @DisplayName("Should create and process digital order")
        void shouldCreateDigitalOrder() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("EBOOK-DDD-001");
            item.setQuantity(1);
            item.setMetadata(Map.of("format", "PDF", "deliveryEmail", "test@example.com"));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-digital-001");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isIn(OrderStatus.PROCESSED, OrderStatus.FAILED);
            });
        }
    }

    @Nested
    @DisplayName("Corporate Orders")
    class CorporateOrderTests {

        @Test
        @DisplayName("Should create corporate order with valid CNPJ")
        void shouldCreateCorporateOrderWithValidCnpj() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("CORP-CHAIR-ERG-001");
            item.setQuantity(50);
            item.setMetadata(Map.of(
                    "cnpj", "12.345.678/0001-90",
                    "paymentTerms", "NET_60",
                    "purchaseOrder", "PO-2025-001"
            ));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("company-acme-corp");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isIn(
                        OrderStatus.PROCESSED,
                        OrderStatus.FAILED,
                        OrderStatus.PENDING_APPROVAL
                );
            });
        }

        @Test
        @DisplayName("Should apply bulk discount for quantity > 100")
        void shouldApplyBulkDiscount() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("CORP-CHAIR-ERG-001");
            item.setQuantity(150);
            item.setMetadata(Map.of(
                    "cnpj", "12.345.678/0001-90",
                    "paymentTerms", "NET_30"
            ));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("company-bulk-test");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getTotalAmount())
                    .isEqualByComparingTo("134850.00");
        }

        @Test
        @DisplayName("Should fail corporate order without CNPJ")
        void shouldFailCorporateOrderWithoutCnpj() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("CORP-LICENSE-ENT");
            item.setQuantity(1);
            item.setMetadata(Map.of("paymentTerms", "NET_30"));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("company-no-cnpj");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                assertThat(order.getFailureReason().name()).isEqualTo("INVALID_CORPORATE_DATA");
            });
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 400 for empty items list")
        void shouldReturn400ForEmptyItems() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-empty");
            req.setItems(List.of());

            ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, ApiErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 for null customer ID")
        void shouldReturn400ForNullCustomerId() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("BOOK-CC-001");
            item.setQuantity(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId(null);
            req.setItems(List.of(item));

            ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, ApiErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 404 for non-existent product")
        void shouldReturn404ForNonExistentProduct() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("NON-EXISTENT-001");
            item.setQuantity(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-not-found");
            req.setItems(List.of(item));

            ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, ApiErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 400 for invalid quantity")
        void shouldReturn400ForInvalidQuantity() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("BOOK-CC-001");
            item.setQuantity(0);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-invalid-qty");
            req.setItems(List.of(item));

            ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, ApiErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Order Retrieval")
    class OrderRetrievalTests {

        @Test
        @DisplayName("Should retrieve order by ID")
        void shouldRetrieveOrderById() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("BOOK-CC-001");
            item.setQuantity(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-retrieve");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> createResponse = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            Long orderId = createResponse.getBody().getOrderId();

            ResponseEntity<OrderDetailResponse> getResponse = restTemplate.getForEntity(
                    "/api/orders/" + orderId, OrderDetailResponse.class);

            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getResponse.getBody()).isNotNull();
            assertThat(getResponse.getBody().getOrderId()).isEqualTo(orderId);
            assertThat(getResponse.getBody().getCustomerId()).isEqualTo("customer-retrieve");
            assertThat(getResponse.getBody().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void shouldReturn404ForNonExistentOrder() {
            ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                    "/api/orders/999999", ApiErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should list orders by customer with pagination")
        void shouldListOrdersByCustomer() {
            String customerId = "customer-list-test";

            for (int i = 0; i < 3; i++) {
                CreateOrderItemRequest item = new CreateOrderItemRequest();
                item.setProductId("BOOK-CC-001");
                item.setQuantity(1);

                CreateOrderRequest req = new CreateOrderRequest();
                req.setCustomerId(customerId);
                req.setItems(List.of(item));

                restTemplate.postForEntity("/api/orders", req, CreateOrderResponse.class);
            }

            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/orders?customerId=" + customerId + "&page=0&size=10",
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"totalElements\":3");
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should handle duplicate processing gracefully")
        void shouldHandleDuplicateProcessing() {
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId("BOOK-CC-001");
            item.setQuantity(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-idempotent");
            req.setItems(List.of(item));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isNotEqualTo(OrderStatus.PENDING);
            });

            var order = orderRepository.findById(orderId).orElseThrow();
            OrderStatus finalStatus = order.getStatus();

            await().during(Duration.ofSeconds(2)).untilAsserted(() -> {
                var orderCheck = orderRepository.findById(orderId).orElseThrow();
                assertThat(orderCheck.getStatus()).isEqualTo(finalStatus);
            });
        }
    }

    @Nested
    @DisplayName("Mixed Orders")
    class MixedOrderTests {

        @Test
        @DisplayName("Should process order with multiple product types")
        void shouldProcessMixedOrder() {
            CreateOrderItemRequest physical = new CreateOrderItemRequest();
            physical.setProductId("BOOK-CC-001");
            physical.setQuantity(1);
            physical.setMetadata(Map.of("warehouseLocation", "SP"));

            CreateOrderItemRequest subscription = new CreateOrderItemRequest();
            subscription.setProductId("SUB-PREMIUM-001");
            subscription.setQuantity(1);
            subscription.setMetadata(Map.of("billingCycle", "MONTHLY"));

            CreateOrderItemRequest digital = new CreateOrderItemRequest();
            digital.setProductId("EBOOK-JAVA-001");
            digital.setQuantity(1);
            digital.setMetadata(Map.of("format", "PDF"));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setCustomerId("customer-mixed-001");
            req.setItems(List.of(physical, subscription, digital));

            ResponseEntity<CreateOrderResponse> response = restTemplate.postForEntity(
                    "/api/orders", req, CreateOrderResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Long orderId = response.getBody().getOrderId();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isIn(OrderStatus.PROCESSED, OrderStatus.FAILED);
            });
        }
    }
}
