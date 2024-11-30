package com.simulador.infraestructura.entidades;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.simulador.infraestructura.models.Order;
import com.simulador.infraestructura.monitores.OrderMonitor;
import com.simulador.infraestructura.monitores.CustomersMonitor;
import com.simulador.config.Config;
import javafx.geometry.Point2D;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;

public class Waiter extends Component {
    private static final AtomicInteger orderIdGenerator = new AtomicInteger(0);
    private final Object taskLock = new Object();
    private final Object stateLock = new Object();

    private final OrderMonitor orderQueueMonitor;
    private final CustomersMonitor customerQueueMonitor;
    private final Point2D restPosition;
    private Point2D targetPosition;
    private boolean isMoving = false;
    private boolean isBusy = false;
    private WaiterState state = WaiterState.RESTING;
    private final Queue<Task> taskQueue = new LinkedList<>();
    private static final double SPEED = Config.WAITER_SPEED;
    private final List<Entity> tables;

    public enum WaiterState {
        RESTING,
        MOVING_TO_TABLE,
        TAKING_ORDER,
        MOVING_TO_KITCHEN,
        DELIVERING_ORDER,
        RETURNING_TO_REST
    }

    private static class Task {
        final WaiterState targetState;
        final Point2D targetPos;
        final Runnable onComplete;

        Task(WaiterState state, Point2D pos, Runnable onComplete) {
            this.targetState = state;
            this.targetPos = pos;
            this.onComplete = onComplete;
        }
    }

    public Waiter(int id, OrderMonitor orderQueueMonitor, CustomersMonitor customerQueueMonitor,
                  Point2D restPosition, List<Entity> tables) {
        this.orderQueueMonitor = orderQueueMonitor;
        this.customerQueueMonitor = customerQueueMonitor;
        this.restPosition = restPosition;
        this.tables = tables;
    }

    @Override
    public void onAdded() {
        entity.setPosition(restPosition);
        startWaiterBehavior();
    }

    @Override
    public void onUpdate(double tpf) {
        if (isMoving && targetPosition != null) {
            Point2D currentPos = entity.getPosition();
            Point2D direction = targetPosition.subtract(currentPos);

            double distance = direction.magnitude();
            if (distance < SPEED * tpf) {
                entity.setPosition(targetPosition);
                onTargetReached();
            } else {
                direction = direction.normalize().multiply(SPEED * tpf);
                entity.translate(direction.getX(), direction.getY());
            }
        }
    }

    private void startWaiterBehavior() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    synchronized (stateLock) {
                        if (!isBusy) {
                            if (!checkAndDeliverReadyOrders()) {
                                if (customerQueueMonitor.hasWaitingCustomers()) {
                                    CustomersMonitor.CustomerRequest request =
                                            customerQueueMonitor.getNextCustomer();
                                    if (request != null) {
                                        serveCustomer(request.customer, request.tableNumber);
                                    }
                                } else if (state != WaiterState.RESTING && !isMoving && taskQueue.isEmpty()) {
                                    moveToRest();
                                }
                            }
                        }
                        stateLock.wait(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void serveCustomer(Customer customer, int tableNumber) {
        synchronized (stateLock) {
            isBusy = true;
            addTask(new Task(
                    WaiterState.MOVING_TO_TABLE,
                    calculateTablePosition(tableNumber),
                    () -> takeOrderFromCustomer(tableNumber)
            ));
        }
    }

    private void addTask(Task task) {
        synchronized (taskLock) {
            taskQueue.add(task);
            if (!isMoving) {
                startNextTask();
            }
        }
    }

    private void startNextTask() {
        synchronized (taskLock) {
            if (!taskQueue.isEmpty() && !isMoving) {
                Task task = taskQueue.peek();
                if (task != null) {
                    state = task.targetState;
                    targetPosition = task.targetPos;
                    isMoving = true;
                }
            }
        }
    }

    private void onTargetReached() {
        synchronized (taskLock) {
            isMoving = false;
            Task currentTask = taskQueue.poll();
            if (currentTask != null) {
                currentTask.onComplete.run();
            }
            if (!taskQueue.isEmpty()) {
                startNextTask();
            }
        }
    }

    private void takeOrderFromCustomer(int tableNumber) {
        Order order = new Order(orderIdGenerator.incrementAndGet(), tableNumber);

        Point2D kitchenPos = new Point2D(Config.KITCHEN_X - 50, Config.KITCHEN_Y);
        addTask(new Task(
                WaiterState.MOVING_TO_KITCHEN,
                kitchenPos,
                () -> {
                    orderQueueMonitor.addOrder(order);
                    resetState();
                }
        ));
    }

    private void deliverOrder(Order order) {
        synchronized (stateLock) {
            isBusy = true;
            int tableNumber = order.getTableNumber();

            Point2D kitchenPos = new Point2D(Config.KITCHEN_X - 50, Config.KITCHEN_Y);
            addTask(new Task(
                    WaiterState.MOVING_TO_KITCHEN,
                    kitchenPos,
                    () -> addTask(new Task(
                            WaiterState.DELIVERING_ORDER,
                            calculateTablePosition(tableNumber),
                            () -> {
                                for (Entity tableEntity : tables) {
                                    Table table = tableEntity.getComponent(Table.class);
                                    if (table != null && table.getNumber() == tableNumber) {
                                        Customer customer = table.getCurrentCustomer();
                                        if (customer != null) {
                                            customer.startEating();
                                        }
                                        break;
                                    }
                                }
                                resetState();
                            }
                    ))
            ));
        }
    }

    private boolean checkAndDeliverReadyOrders() {
        try {
            synchronized (stateLock) {
                if (!isBusy) {
                    for (int i = 0; i < Config.TOTAL_TABLES; i++) {
                        Order readyOrder = orderQueueMonitor.checkReadyOrder(i);
                        if (readyOrder != null) {
                            deliverOrder(readyOrder);
                            return true;
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private void moveToRest() {
        synchronized (taskLock) {
            if (!isMoving && taskQueue.isEmpty()) {
                addTask(new Task(
                        WaiterState.RETURNING_TO_REST,
                        restPosition,
                        () -> state = WaiterState.RESTING
                ));
            }
        }
    }

    private Point2D calculateTablePosition(int tableNumber) {
        int row = tableNumber / 5;
        int col = tableNumber % 5;
        return new Point2D(
                300 + col * (Config.SPRITE_SIZE * 2),
                100 + row * (Config.SPRITE_SIZE * 2)
        );
    }

    private void resetState() {
        synchronized (stateLock) {
            isBusy = false;
        }
    }
}