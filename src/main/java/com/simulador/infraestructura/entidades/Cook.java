package com.simulador.infraestructura.entidades;

import com.simulador.infraestructura.models.Order;
import com.simulador.infraestructura.monitores.OrderMonitor;
import com.simulador.infraestructura.monitores.OrderMonitor;

public class Cook implements Runnable {
    private final OrderMonitor orderQueueMonitor;
    private volatile boolean isResting;
    private Order currentOrder;

    public Cook(int id, OrderMonitor orderQueueMonitor) {
        this.orderQueueMonitor = orderQueueMonitor;
        this.isResting = true;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                currentOrder = orderQueueMonitor.getNextOrder();
                isResting = false;

                Thread.sleep(currentOrder.getPreparationTime());

                orderQueueMonitor.markOrderAsReady(currentOrder);
                currentOrder = null;
                isResting = true;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}