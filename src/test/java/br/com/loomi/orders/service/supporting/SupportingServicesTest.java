package br.com.loomi.orders.service.supporting;

import br.com.loomi.orders.exception.BusinessException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class SupportingServicesTest {

    @Nested
    @DisplayName("Inventory Service Tests")
    class InventoryServiceTests {

        private InventoryService inventoryService;

        @BeforeEach
        void setUp() {
            inventoryService = new InventoryService();
        }

        @Test
        @DisplayName("Should initialize stock and reserve successfully")
        void shouldInitializeAndReserveStock() {
            inventoryService.initStockIfAbsent("PRODUCT-001", 100);
            
            assertThat(inventoryService.getStock("PRODUCT-001")).isEqualTo(100);
            
            inventoryService.reserve("PRODUCT-001", 30);
            
            assertThat(inventoryService.getStock("PRODUCT-001")).isEqualTo(70);
        }

        @Test
        @DisplayName("Should throw exception when stock is insufficient")
        void shouldThrowExceptionWhenStockInsufficient() {
            inventoryService.initStockIfAbsent("PRODUCT-001", 10);

            assertThatThrownBy(() -> inventoryService.reserve("PRODUCT-001", 20))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("OUT_OF_STOCK");
                    });
        }

        @Test
        @DisplayName("Should return zero for non-existent product")
        void shouldReturnZeroForNonExistentProduct() {
            assertThat(inventoryService.getStock("NON-EXISTENT")).isZero();
        }

        @Test
        @DisplayName("Should not reinitialize stock if already set")
        void shouldNotReinitializeStock() {
            inventoryService.initStockIfAbsent("PRODUCT-001", 100);
            inventoryService.initStockIfAbsent("PRODUCT-001", 50);
            
            assertThat(inventoryService.getStock("PRODUCT-001")).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Subscription Service Tests")
    class SubscriptionServiceTests {

        private SubscriptionService subscriptionService;

        @BeforeEach
        void setUp() {
            subscriptionService = new SubscriptionService();
        }

        @Test
        @DisplayName("Should activate subscription successfully")
        void shouldActivateSubscription() {
            assertThatCode(() -> 
                subscriptionService.validateAndActivate("customer-001", "SUB-PREMIUM-001")
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception for duplicate subscription")
        void shouldThrowExceptionForDuplicateSubscription() {
            subscriptionService.validateAndActivate("customer-001", "SUB-PREMIUM-001");

            assertThatThrownBy(() -> 
                subscriptionService.validateAndActivate("customer-001", "SUB-PREMIUM-001")
            ).isInstanceOf(BusinessException.class)
             .satisfies(ex -> {
                 BusinessException be = (BusinessException) ex;
                 assertThat(be.getCode()).isEqualTo("DUPLICATE_ACTIVE_SUBSCRIPTION");
             });
        }

        @Test
        @DisplayName("Should throw exception when subscription limit exceeded")
        void shouldThrowExceptionWhenLimitExceeded() {
            for (int i = 1; i <= 5; i++) {
                subscriptionService.validateAndActivate("customer-001", "SUB-" + i);
            }

            assertThatThrownBy(() -> 
                subscriptionService.validateAndActivate("customer-001", "SUB-6")
            ).isInstanceOf(BusinessException.class)
             .satisfies(ex -> {
                 BusinessException be = (BusinessException) ex;
                 assertThat(be.getCode()).isEqualTo("SUBSCRIPTION_LIMIT_EXCEEDED");
             });
        }

        @Test
        @DisplayName("Should throw exception for incompatible subscriptions - Enterprise + Basic")
        void shouldThrowExceptionForIncompatibleEnterprisePlusBasic() {
            subscriptionService.validateAndActivate("customer-001", "SUB-ENTERPRISE-001");

            assertThatThrownBy(() -> 
                subscriptionService.validateAndActivate("customer-001", "SUB-BASIC-001")
            ).isInstanceOf(BusinessException.class)
             .satisfies(ex -> {
                 BusinessException be = (BusinessException) ex;
                 assertThat(be.getCode()).isEqualTo("INCOMPATIBLE_SUBSCRIPTIONS");
             });
        }

        @Test
        @DisplayName("Should throw exception for incompatible subscriptions - Basic + Enterprise")
        void shouldThrowExceptionForIncompatibleBasicPlusEnterprise() {
            subscriptionService.validateAndActivate("customer-001", "SUB-BASIC-001");

            assertThatThrownBy(() -> 
                subscriptionService.validateAndActivate("customer-001", "SUB-ENTERPRISE-001")
            ).isInstanceOf(BusinessException.class)
             .satisfies(ex -> {
                 BusinessException be = (BusinessException) ex;
                 assertThat(be.getCode()).isEqualTo("INCOMPATIBLE_SUBSCRIPTIONS");
             });
        }

        @Test
        @DisplayName("Should allow different customers to have same subscription")
        void shouldAllowDifferentCustomersSameSubscription() {
            assertThatCode(() -> {
                subscriptionService.validateAndActivate("customer-001", "SUB-PREMIUM-001");
                subscriptionService.validateAndActivate("customer-002", "SUB-PREMIUM-001");
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Corporate Credit Service Tests")
    class CorporateCreditServiceTests {

        private CorporateCreditService creditService;

        @BeforeEach
        void setUp() {
            creditService = new CorporateCreditService();
        }

        @Test
        @DisplayName("Should reserve credit successfully within limit")
        void shouldReserveCreditWithinLimit() {
            assertThatCode(() -> 
                creditService.validateAndReserve("company-001", BigDecimal.valueOf(50000))
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception when credit limit exceeded")
        void shouldThrowExceptionWhenCreditLimitExceeded() {
            BigDecimal amount = BigDecimal.valueOf(150000);

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> creditService.validateAndReserve("company-001", amount)
            );

            assertThat(exception.getCode()).isEqualTo("CREDIT_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("Should accumulate credit usage")
        void shouldAccumulateCreditUsage() {
            BigDecimal firstAmount = BigDecimal.valueOf(60_000);
            BigDecimal secondAmount = BigDecimal.valueOf(50_000);

            creditService.validateAndReserve("company-001", firstAmount);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> creditService.validateAndReserve("company-001", secondAmount)
            );

            assertThat(exception.getCode()).isEqualTo("CREDIT_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("Should allow different companies to use credit independently")
        void shouldAllowDifferentCompaniesIndependently() {
            assertThatCode(() -> {
                creditService.validateAndReserve("company-001", BigDecimal.valueOf(80000));
                creditService.validateAndReserve("company-002", BigDecimal.valueOf(80000));
            }).doesNotThrowAnyException();
        }
    }
}
