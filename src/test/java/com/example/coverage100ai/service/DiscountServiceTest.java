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

    // --- calculateDiscountRate の未カバーブランチ (/coverage コマンドで生成) ---

    @Test
    void test_calculateDiscountRate_nullOrder_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> discountService.calculateDiscountRate(null));
    }

    @Test
    void test_calculateDiscountRate_premiumOnly_returnsPremiumDiscount() {
        // プレミアム会員 かつ 少量注文(bulk=false) → 15%
        Order order = new Order("2", "customer2", 5, 100.0, true);
        assertEquals(0.15, discountService.calculateDiscountRate(order));
    }

    @Test
    void test_calculateDiscountRate_bulkOnly_returnsBulkDiscount() {
        // 非プレミアム かつ 大量注文 → 10%
        Order order = new Order("3", "customer3", 10, 100.0, false);
        assertEquals(0.10, discountService.calculateDiscountRate(order));
    }

    @Test
    void test_calculateDiscountRate_noDiscount() {
        // 非プレミアム かつ 少量注文 → 0%
        Order order = new Order("4", "customer4", 3, 100.0, false);
        assertEquals(0.0, discountService.calculateDiscountRate(order));
    }

    // --- getStatusMessage の未カバーブランチ (/coverage コマンドで生成) ---

    @Test
    void test_getStatusMessage_nullOrder_returnsNoInfoMessage() {
        assertEquals("注文情報がありません", discountService.getStatusMessage(null));
    }

    @Test
    void test_getStatusMessage_confirmed() {
        Order order = new Order("1", "c1", 1, 100.0, false);
        order.setStatus(Order.Status.CONFIRMED);
        assertEquals("注文が確定しました", discountService.getStatusMessage(order));
    }

    @Test
    void test_getStatusMessage_shipped() {
        Order order = new Order("1", "c1", 1, 100.0, false);
        order.setStatus(Order.Status.SHIPPED);
        assertEquals("発送済みです", discountService.getStatusMessage(order));
    }

    @Test
    void test_getStatusMessage_delivered() {
        Order order = new Order("1", "c1", 1, 100.0, false);
        order.setStatus(Order.Status.DELIVERED);
        assertEquals("配達完了しました", discountService.getStatusMessage(order));
    }

    @Test
    void test_getStatusMessage_cancelled() {
        Order order = new Order("1", "c1", 1, 100.0, false);
        order.setStatus(Order.Status.CANCELLED);
        assertEquals("注文はキャンセルされました", discountService.getStatusMessage(order));
    }
}
