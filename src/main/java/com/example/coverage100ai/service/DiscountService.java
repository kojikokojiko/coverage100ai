package com.example.coverage100ai.service;

import com.example.coverage100ai.model.Order;
import org.springframework.stereotype.Service;

/**
 * 注文に対する割引を計算するサービス。
 * 分岐が多く、カバレッジ100%が大変なクラス。
 */
@Service
public class DiscountService {

    private static final double PREMIUM_DISCOUNT = 0.15;
    private static final double BULK_DISCOUNT = 0.10;
    private static final double COMBINED_DISCOUNT = 0.20;

    /**
     * 割引率を計算する。
     * - プレミアム会員 かつ 大量注文(10個以上) → 20%引き
     * - プレミアム会員のみ → 15%引き
     * - 大量注文のみ → 10%引き
     * - それ以外 → 割引なし
     */
    public double calculateDiscountRate(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }

        boolean isBulk = order.getQuantity() >= 10;

        if (order.isPremiumCustomer() && isBulk) {
            return COMBINED_DISCOUNT;
        } else if (order.isPremiumCustomer()) {
            return PREMIUM_DISCOUNT;
        } else if (isBulk) {
            return BULK_DISCOUNT;
        } else {
            return 0.0;
        }
    }

    /**
     * 割引後の合計金額を計算する。
     */
    public double calculateFinalPrice(Order order) {
        double discountRate = calculateDiscountRate(order);
        double basePrice = order.getQuantity() * order.getUnitPrice();
        return basePrice * (1 - discountRate);
    }

    /**
     * 注文ステータスに応じたメッセージを返す。
     */
    public String getStatusMessage(Order order) {
        if (order == null) {
            return "注文情報がありません";
        }

        return switch (order.getStatus()) {
            case PENDING -> "注文受付中です";
            case CONFIRMED -> "注文が確定しました";
            case SHIPPED -> "発送済みです";
            case DELIVERED -> "配達完了しました";
            case CANCELLED -> "注文はキャンセルされました";
        };
    }
}
