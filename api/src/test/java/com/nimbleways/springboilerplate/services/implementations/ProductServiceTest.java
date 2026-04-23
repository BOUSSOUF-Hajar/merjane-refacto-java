package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@UnitTest
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProductService productService;

    // ==================== NORMAL PRODUCTS ====================

    @Test
    void normalProduct_withStock_shouldDecrementAvailable() {
        Product product = createProduct(ProductType.NORMAL, 30, 15);

        productService.processProduct(product);

        assertEquals(29, product.getAvailable());
        verify(productRepository).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    void normalProduct_outOfStock_withLeadTime_shouldNotifyDelay() {
        Product product = createProduct(ProductType.NORMAL, 0, 15);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendDelayNotification(15, product.getName());
    }

    @Test
    void normalProduct_outOfStock_noLeadTime_shouldDoNothing() {
        Product product = createProduct(ProductType.NORMAL, 0, 0);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verifyNoInteractions(productRepository);
        verifyNoInteractions(notificationService);
    }

    // ==================== SEASONAL PRODUCTS ====================

    @Test
    void seasonalProduct_inSeason_withStock_shouldDecrementAvailable() {
        Product product = createSeasonalProduct(30, 15,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(50));

        productService.processProduct(product);

        assertEquals(29, product.getAvailable());
        verify(productRepository).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    void seasonalProduct_inSeason_outOfStock_resupplyBeforeSeasonEnd_shouldNotifyDelay() {
        Product product = createSeasonalProduct(0, 15,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(50));

        productService.processProduct(product);

        verify(productRepository).save(product);
        verify(notificationService).sendDelayNotification(15, product.getName());
    }

    @Test
    void seasonalProduct_resupplyAfterSeasonEnd_shouldMarkOutOfStockAndNotify() {
        Product product = createSeasonalProduct(0, 15,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendOutOfStockNotification(product.getName());
    }

    @Test
    void seasonalProduct_beforeSeasonStart_shouldNotifyOutOfStock() {
        Product product = createSeasonalProduct(30, 15,
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(90));

        productService.processProduct(product);

        verify(productRepository).save(product);
        verify(notificationService).sendOutOfStockNotification(product.getName());
    }

    @Test
    void seasonalProduct_outOfStock_resupplyAfterSeasonEnd_shouldSetAvailableToZero() {
        Product product = createSeasonalProduct(0, 100,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendOutOfStockNotification(product.getName());
    }

    // ==================== EXPIRABLE PRODUCTS ====================

    @Test
    void expirableProduct_withStock_notExpired_shouldDecrementAvailable() {
        Product product = createExpirableProduct(10, 15, LocalDate.now().plusDays(30));

        productService.processProduct(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    void expirableProduct_withStock_expired_shouldMarkOutOfStockAndNotifyExpiration() {
        LocalDate expiryDate = LocalDate.now().minusDays(5);
        Product product = createExpirableProduct(10, 15, expiryDate);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendExpirationNotification(product.getName(), expiryDate);
    }

    @Test
    void expirableProduct_outOfStock_notExpired_shouldNotifyExpiration() {
        LocalDate expiryDate = LocalDate.now().plusDays(30);
        Product product = createExpirableProduct(0, 15, expiryDate);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification(product.getName(), expiryDate);
    }

    @Test
    void expirableProduct_outOfStock_expired_shouldNotifyExpiration() {
        LocalDate expiryDate = LocalDate.now().minusDays(10);
        Product product = createExpirableProduct(0, 15, expiryDate);

        productService.processProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification(product.getName(), expiryDate);
    }

    // ==================== HELPER METHODS ====================

    private Product createProduct(ProductType type, int available, int leadTime) {
        return new Product(null, leadTime, available, type, "Test Product", null, null, null);
    }

    private Product createSeasonalProduct(int available, int leadTime,
                                           LocalDate seasonStart, LocalDate seasonEnd) {
        return new Product(null, leadTime, available, ProductType.SEASONAL,
                "Seasonal Product", null, seasonStart, seasonEnd);
    }

    private Product createExpirableProduct(int available, int leadTime, LocalDate expiryDate) {
        return new Product(null, leadTime, available, ProductType.EXPIRABLE,
                "Expirable Product", expiryDate, null, null);
    }
}
