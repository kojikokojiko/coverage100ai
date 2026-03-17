package com.example.coverage100ai.service;

import com.example.coverage100ai.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscountServiceTest {

    private final DiscountService discountService = new DiscountService();

    @Test
    void test_calculateDiscountRate_premiumAndBulk() {
        Order order = new Order("1", "customer1", 10, 100.0, true);
        assertEquals(0.20, discountService.calculateDiscountRate(order));
    }

    @Test
    void test_calculateFinalPrice_basic() {
        Order order = new Order("1", "customer1", 5, 100.0, false);
        assertEquals(500.0, discountService.calculateFinalPrice(order));
    }

    @Test
    void test_getStatusMessage_pending() {
        Order order = new Order("1", "c1", 1, 100.0, false);
        assertEquals("注文受付中です", discountService.getStatusMessage(order));
    }
}
