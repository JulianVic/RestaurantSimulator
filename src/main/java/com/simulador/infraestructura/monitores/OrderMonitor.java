package com.simulador.infraestructura.monitores;

import com.simulador.infraestructura.models.Order;
import com.simulador.infraestructura.models.OrderStatus;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class OrderMonitor {
    private final Queue<Order> pendingOrders;
    private final Queue<Order> inProcessOrders;
    private final Queue<Order> readyOrders;
    private final ReentrantLock lock;
    private final Condition orderAvailable;
    private final Condition foodReady;

    public OrderMonitor() {
        pendingOrders = new LinkedList<>();
        inProcessOrders = new LinkedList<>();
        readyOrders = new LinkedList<>();
        lock = new ReentrantLock();
        orderAvailable = lock.newCondition();
        foodReady = lock.newCondition();
    }

    public void addOrder(Order order) {
        lock.lock();
        try {
            pendingOrders.add(order);
            orderAvailable.signal();
        } finally {
            lock.unlock();
        }
    }

    public Order getNextOrder() throws InterruptedException {
        lock.lock();
        try {
            while (pendingOrders.isEmpty()) {
                orderAvailable.await();
            }
            Order order = pendingOrders.poll();
            order.setStatus(OrderStatus.IN_PROCESS);
            inProcessOrders.add(order);
            return order;
        } finally {
            lock.unlock();
        }
    }

    public void markOrderAsReady(Order order) {
        lock.lock();
        try {
            order.setStatus(OrderStatus.READY);
            inProcessOrders.remove(order);
            readyOrders.add(order);
            foodReady.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Order checkReadyOrder(int tableNumber) throws InterruptedException {
        lock.lock();
        try {
            for (Order order : readyOrders) {
                if (order.getTableNumber() == tableNumber) {
                    readyOrders.remove(order);
                    order.setStatus(OrderStatus.DELIVERED);
                    return order;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }
}