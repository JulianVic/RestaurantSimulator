package com.simulador.infraestructura.models;

public class Order {
    private final int id;
    private final int tableNumber;
    private final long preparationTime;
    private OrderStatus status;


    public Order(int id, int tableNumber) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.status = OrderStatus.PENDING;
        this.preparationTime = (long) (Math.random() * 5000 + 3000); // Between 3 and 8 seconds
    }

    public int getId() { return id; }
    public int getTableNumber() { return tableNumber; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public long getPreparationTime() { return preparationTime; }
}