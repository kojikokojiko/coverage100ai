package com.example.coverage100ai.model;

public class Order {

    public enum Status {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    private String id;
    private String customerId;
    private int quantity;
    private double unitPrice;
    private Status status;
    private boolean isPremiumCustomer;

    public Order(String id, String customerId, int quantity, double unitPrice, boolean isPremiumCustomer) {
        this.id = id;
        this.customerId = customerId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = Status.PENDING;
        this.isPremiumCustomer = isPremiumCustomer;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public Status getStatus() { return status; }
    public boolean isPremiumCustomer() { return isPremiumCustomer; }
    public void setStatus(Status status) { this.status = status; }
}
