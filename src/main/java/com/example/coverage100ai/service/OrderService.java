package com.example.coverage100ai.service;

import com.example.coverage100ai.model.Order;
import org.springframework.stereotype.Service;

/**
 * 注文処理サービス。
 * キャンセル・確定・バリデーションなど複数の分岐がある。
 */
@Service
public class OrderService {

    private final DiscountService discountService;

    public OrderService(DiscountService discountService) {
        this.discountService = discountService;
    }

    /**
     * 注文を確定する。
     * - PENDING 以外はキャンセル不可
     * - 数量が0以下はNG
     * - 単価が0以下はNG
     */
    public Order confirmOrder(Order order) {
        validateOrder(order);

        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalStateException(
                "確定できるのはPENDING状態の注文のみです。現在のステータス: " + order.getStatus()
            );
        }

        order.setStatus(Order.Status.CONFIRMED);
        return order;
    }

    /**
     * 注文をキャンセルする。
     * - DELIVERED / SHIPPED はキャンセル不可
     * - すでに CANCELLED の場合は何もしない
     */
    public boolean cancelOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }

        if (order.getStatus() == Order.Status.CANCELLED) {
            return false; // すでにキャンセル済み
        }

        if (order.getStatus() == Order.Status.DELIVERED
                || order.getStatus() == Order.Status.SHIPPED) {
            throw new IllegalStateException("発送済み・配達済みの注文はキャンセルできません");
        }

        order.setStatus(Order.Status.CANCELLED);
        return true;
    }

    /**
     * 注文サマリーを返す。
     * プレミアム会員には割引情報を付加する。
     */
    public String getOrderSummary(Order order) {
        validateOrder(order);

        double finalPrice = discountService.calculateFinalPrice(order);
        double discountRate = discountService.calculateDiscountRate(order);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("注文ID: %s%n", order.getId()));
        sb.append(String.format("数量: %d / 単価: %.0f円%n", order.getQuantity(), order.getUnitPrice()));
        sb.append(String.format("合計: %.0f円%n", finalPrice));

        if (discountRate > 0) {
            sb.append(String.format("割引適用: %.0f%%%n", discountRate * 100));
        }

        return sb.toString();
    }

    private void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("数量は1以上である必要があります");
        }
        if (order.getUnitPrice() <= 0) {
            throw new IllegalArgumentException("単価は0より大きい必要があります");
        }
    }
}
