package com.simulador.infraestructura.monitores;

import com.simulador.infraestructura.entidades.Customer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomersMonitor {
    private final Queue<CustomerRequest> waitingCustomers;
    private final ReentrantLock lock;
    private final Condition customerAvailable;

    public static class CustomerRequest {
        public final Customer customer;
        public final int tableNumber;
        public final long arrivalTime;

        public CustomerRequest(Customer customer, int tableNumber) {
            this.customer = customer;
            this.tableNumber = tableNumber;
            this.arrivalTime = System.currentTimeMillis();
        }
    }

    public CustomersMonitor() {
        waitingCustomers = new LinkedList<>();
        lock = new ReentrantLock();
        customerAvailable = lock.newCondition();
    }

    public void addCustomer(Customer customer, int tableNumber) {
        lock.lock();
        try {
            waitingCustomers.add(new CustomerRequest(customer, tableNumber));
            customerAvailable.signal();
        } finally {
            lock.unlock();
        }
    }

    public CustomerRequest getNextCustomer() throws InterruptedException {
        lock.lock();
        try {
            while (waitingCustomers.isEmpty()) {
                customerAvailable.await();
            }
            return waitingCustomers.poll();
        } finally {
            lock.unlock();
        }
    }

    public boolean hasWaitingCustomers() {
        lock.lock();
        try {
            return !waitingCustomers.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}