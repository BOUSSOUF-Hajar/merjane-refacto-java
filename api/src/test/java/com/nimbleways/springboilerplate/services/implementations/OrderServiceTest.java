package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@UnitTest
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void processOrder_shouldProcessAllProducts() {
        Product normal = new Product(1L, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null);
        Product seasonal = new Product(2L, 15, 30, ProductType.SEASONAL, "Watermelon", null,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));

        Order order = new Order(1L, Set.of(normal, seasonal));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.processOrder(1L);

        assertEquals(1L, result.getId());
        verify(productService).processProduct(normal);
        verify(productService).processProduct(seasonal);
    }

    @Test
    void processOrder_orderNotFound_shouldThrowException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.processOrder(99L));
    }
}
