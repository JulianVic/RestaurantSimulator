package com.simulador.infraestructura.entidades;

import com.almasb.fxgl.entity.component.Component;
import com.simulador.infraestructura.monitores.RestaurantMonitor;
import com.simulador.infraestructura.models.CustomerStats;
import javafx.geometry.Point2D;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Receptionist extends Component {
    private final Point2D position;
    private final RestaurantMonitor restaurantMonitor;
    private final Queue<Customer> waitingCustomers;
    private final ReentrantLock lock;
    private final Condition customerWaiting;
    private boolean isBusy;
    private Customer currentCustomer;
    private final CustomerStats customerStats;

    public Receptionist(RestaurantMonitor restaurantMonitor, Point2D position, CustomerStats customerStats) {
        this.restaurantMonitor = restaurantMonitor;
        this.position = position;
        this.customerStats = customerStats;
        this.waitingCustomers = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.customerWaiting = lock.newCondition();
        this.isBusy = false;

        startReceptionistBehavior();
    }

    private void startReceptionistBehavior() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Customer customer = getNextCustomer();
                    if (customer != null) {
                        handleCustomer(customer);
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void addCustomerToQueue(Customer customer) {
        lock.lock();
        try {
            waitingCustomers.add(customer);
            customerWaiting.signal();
        } finally {
            lock.unlock();
        }
    }

    private Customer getNextCustomer() throws InterruptedException {
        lock.lock();
        try {
            while (waitingCustomers.isEmpty()) {
                customerWaiting.await();
            }
            currentCustomer = waitingCustomers.poll();
            isBusy = true;
            return currentCustomer;
        } finally {
            lock.unlock();
        }
    }

    private void handleCustomer(Customer customer) {
        try {
            Thread.sleep(200);

            int tableNumber = restaurantMonitor.findAvailableTable();

            if (tableNumber != -1) {
                restaurantMonitor.occupyTable(tableNumber);
                customer.assignTable(tableNumber);
            } else {
                customerStats.incrementWaitingForTable();
                customer.waitForTable();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.lock();
            try {
                currentCustomer = null;
                isBusy = false;
            } finally {
                lock.unlock();
            }
        }
    }

    public boolean isBusy() {
        lock.lock();
        try {
            return isBusy;
        } finally {
            lock.unlock();
        }
    }

    public Customer getCurrentCustomer() {
        lock.lock();
        try {
            return currentCustomer;
        } finally {
            lock.unlock();
        }
    }

    public Point2D getPosition() {
        return position;
    }

    @Override
    public void onUpdate(double tpf) {

    }
}