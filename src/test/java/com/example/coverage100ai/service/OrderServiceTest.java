package com.example.coverage100ai.service;

import com.example.coverage100ai.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void test_confirmOrder_pendingOrder_returnsConfirmed() {
        Order order = new Order("1", "c1", 3, 100.0, false);
        Order result = orderService.confirmOrder(order);
        assertEquals(Order.Status.CONFIRMED, result.getStatus());
    }

    @Test
    void test_confirmOrder_nonPendingOrder_throwsException() {
        Order order = new Order("1", "c1", 3, 100.0, false);
        order.setStatus(Order.Status.CONFIRMED);
        assertThrows(IllegalStateException.class, () -> orderService.confirmOrder(order));
    }

    @Test
    void test_confirmOrder_nullOrder_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.confirmOrder(null));
    }

    @Test
    void test_confirmOrder_zeroQuantity_throwsException() {
        Order order = new Order("1", "c1", 0, 100.0, false);
        assertThrows(IllegalArgumentException.class, () -> orderService.confirmOrder(order));
    }

    @Test
    void test_confirmOrder_zeroUnitPrice_throwsException() {
        Order order = new Order("1", "c1", 1, 0.0, false);
        assertThrows(IllegalArgumentException.class, () -> orderService.confirmOrder(order));
    }

    @Test
    void test_cancelOrder_pendingOrder_returnsTrueAndCancelled() {
        Order order = new Order("1", "c1", 3, 100.0, false);
        assertTrue(orderService.cancelOrder(order));
        assertEquals(Order.Status.CANCELLED, order.getStatus());
    }

    @Test
    void test_cancelOrder_alreadyCancelled_returnsFalse() {
        Order order = new Order("1", "c1", 3, 100.0, false);
        order.setStatus(Order.Status.CANCELLED);
        assertFalse(orderService.cancelOrder(order));
    }

    @Test
    void test_cancelOrder_shippedOrder_throwsException() {
        Order order = new Order("1", "c1", 3, 100.0, false);
        order.setStatus(Order.Status.SHIPPED);
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(order));
    }

    @Test
    void test_cancelOrder_deliveredOrder_throwsException() {
        Order order = new Order("1", "c1", 3, 100.0, false);
        order.setStatus(Order.Status.DELIVERED);
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(order));
    }

    @Test
    void test_cancelOrder_nullOrder_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(null));
    }

    @Test
    void test_getOrderSummary_withDiscount_includesDiscountLine() {
        Order order = new Order("1", "c1", 5, 200.0, true);
        when(discountService.calculateFinalPrice(order)).thenReturn(850.0);
        when(discountService.calculateDiscountRate(order)).thenReturn(0.15);

        String summary = orderService.getOrderSummary(order);

        assertTrue(summary.contains("注文ID: 1"));
        assertTrue(summary.contains("割引適用: 15%"));
    }

    @Test
    void test_getOrderSummary_withoutDiscount_noDiscountLine() {
        Order order = new Order("1", "c1", 5, 200.0, false);
        when(discountService.calculateFinalPrice(order)).thenReturn(1000.0);
        when(discountService.calculateDiscountRate(order)).thenReturn(0.0);

        String summary = orderService.getOrderSummary(order);

        assertTrue(summary.contains("注文ID: 1"));
        assertFalse(summary.contains("割引適用"));
    }
}
