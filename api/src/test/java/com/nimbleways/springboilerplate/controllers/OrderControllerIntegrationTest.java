package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NotificationService notificationService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @BeforeEach
        void setUp() {
                orderRepository.deleteAll();
                productRepository.deleteAll();
                reset(notificationService);
        }

        @Test
        void processOrder_normalProductInStock_shouldDecrementAvailable() throws Exception {
                Product product = new Product(null, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null);
                Order order = saveOrderWithProduct(product);

                performProcessOrder(order.getId());

                Product updated = productRepository.findById(product.getId()).get();
                assertEquals(29, updated.getAvailable());
                verifyNoInteractions(notificationService);
        }

        @Test
        void processOrder_normalProductOutOfStock_shouldNotifyDelay() throws Exception {
                Product product = new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null);
                Order order = saveOrderWithProduct(product);

                performProcessOrder(order.getId());

                Product updated = productRepository.findById(product.getId()).get();
                assertEquals(0, updated.getAvailable());
                verify(notificationService).sendDelayNotification(10, "USB Dongle");
        }

        @Test
        void processOrder_expirableProductNotExpired_shouldDecrementAvailable() throws Exception {
                Product product = new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter",
                                LocalDate.now().plusDays(26), null, null);
                Order order = saveOrderWithProduct(product);

                performProcessOrder(order.getId());

                Product updated = productRepository.findById(product.getId()).get();
                assertEquals(29, updated.getAvailable());
                verifyNoInteractions(notificationService);
        }

        @Test
        void processOrder_expirableProductExpired_shouldNotifyExpiration() throws Exception {
                Product product = new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk",
                                LocalDate.now().minusDays(2), null, null);
                Order order = saveOrderWithProduct(product);

                performProcessOrder(order.getId());

                Product updated = productRepository.findById(product.getId()).get();
                assertEquals(0, updated.getAvailable());
                verify(notificationService).sendExpirationNotification(eq("Milk"), any(LocalDate.class));
        }

        @Test
        void processOrder_seasonalProductInSeasonWithStock_shouldDecrementAvailable() throws Exception {
                Product product = new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon", null,
                                LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));
                Order order = saveOrderWithProduct(product);

                performProcessOrder(order.getId());

                Product updated = productRepository.findById(product.getId()).get();
                assertEquals(29, updated.getAvailable());
                verifyNoInteractions(notificationService);
        }

        @Test
        void processOrder_seasonalProductBeforeSeason_shouldNotifyOutOfStock() throws Exception {
                Product product = new Product(null, 15, 30, ProductType.SEASONAL, "Grapes", null,
                                LocalDate.now().plusDays(180), LocalDate.now().plusDays(240));
                Order order = saveOrderWithProduct(product);

                performProcessOrder(order.getId());

                verify(notificationService).sendOutOfStockNotification("Grapes");
        }

        private Order saveOrderWithProduct(Product product) {
                product = productRepository.save(product);
                Order order = new Order();
                order.setItems(Set.of(product));
                return orderRepository.save(order);
        }

        private void performProcessOrder(Long orderId) throws Exception {
                mockMvc.perform(post("/orders/{orderId}/processOrder", orderId)
                                .contentType("application/json"))
                                .andExpect(status().isOk());
        }
}
