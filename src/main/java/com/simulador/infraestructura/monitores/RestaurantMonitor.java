package com.simulador.infraestructura.monitores;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class RestaurantMonitor {
    public static final int TOTAL_TABLES = 10;
    private final boolean[] tables;
    private final ReentrantLock lock;
    private final Condition tableAvailable;

    public RestaurantMonitor() {
        tables = new boolean[TOTAL_TABLES];
        lock = new ReentrantLock();
        tableAvailable = lock.newCondition();
    }

    public int findAvailableTable() {
        lock.lock();
        try {
            for (int i = 0; i < tables.length; i++) {
                if (!tables[i]) {
                    return i;
                }
            }
            return -1;
        } finally {
            lock.unlock();
        }
    }

    public void occupyTable(int tableNumber) {
        lock.lock();
        try {
            tables[tableNumber] = true;
        } finally {
            lock.unlock();
        }
    }

    public void releaseTable(int tableNumber) {
        lock.lock();
        try {
            tables[tableNumber] = false;
            tableAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void waitForAvailableTable() throws InterruptedException {
        lock.lock();
        try {
            while (findAvailableTable() == -1) {
                tableAvailable.await();
            }
        } finally {
            lock.unlock();
        }
    }
}