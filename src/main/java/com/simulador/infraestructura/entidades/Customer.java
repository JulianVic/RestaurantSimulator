package com.simulador.infraestructura.entidades;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.simulador.infraestructura.models.CustomerStats;
import com.simulador.infraestructura.monitores.RestaurantMonitor;
import com.simulador.infraestructura.monitores.OrderMonitor;
import com.simulador.infraestructura.monitores.CustomersMonitor;
import com.simulador.config.Config;
import javafx.geometry.Point2D;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

public class Customer extends Component {
    private final RestaurantMonitor restaurantMonitor;
    private final OrderMonitor orderQueueMonitor;
    private final CustomersMonitor customerQueueMonitor;
    private final CustomerStats customerStats;
    private final List<Entity> tables;
    private int tableNumber = -1;
    private Point2D targetPosition;
    private boolean isMoving = false;
    private CustomerState state = CustomerState.ENTERING;
    private static final double SPEED = Config.CUSTOMER_SPEED;
    private final int id;
    private final Object stateLock = new Object();

    public enum CustomerState {
        ENTERING,
        WAITING_FOR_RECEPTIONIST,
        TALKING_TO_RECEPTIONIST,
        WAITING_FOR_TABLE,
        MOVING_TO_TABLE,
        WAITING_FOR_WAITER,
        WAITING_FOR_FOOD,
        EATING,
        LEAVING
    }

    public Customer(int id, RestaurantMonitor restaurantMonitor, OrderMonitor orderQueueMonitor,
                    CustomersMonitor customerQueueMonitor, CustomerStats customerStats, List<Entity> tables) {
        this.id = id;
        this.restaurantMonitor = restaurantMonitor;
        this.orderQueueMonitor = orderQueueMonitor;
        this.customerQueueMonitor = customerQueueMonitor;
        this.customerStats = customerStats;
        this.tables = tables;
    }

    @Override
    public void onAdded() {
        entity.setPosition(Config.ENTRANCE_X, Config.ENTRANCE_Y);
        moveToReceptionist();
        customerStats.incrementWaitingForTable();
    }

    @Override
    public void onUpdate(double tpf) {
        if (isMoving && targetPosition != null) {
            Point2D currentPos = entity.getPosition();
            Point2D direction = targetPosition.subtract(currentPos);

            if (direction.magnitude() < SPEED * tpf) {
                entity.setPosition(targetPosition);
                onTargetReached();
                return;
            }

            direction = direction.normalize().multiply(SPEED * tpf);
            entity.translate(direction.getX(), direction.getY());
        }
    }

    private void onTargetReached() {
        isMoving = false;
        targetPosition = null;

        switch (state) {
            case ENTERING:
                synchronized (stateLock) {
                    state = CustomerState.WAITING_FOR_RECEPTIONIST;
                    Entity receptionistEntity = findReceptionist();
                    if (receptionistEntity != null) {
                        Receptionist receptionist = receptionistEntity.getComponent(Receptionist.class);
                        receptionist.addCustomerToQueue(this);
                    }
                }
                break;
            case MOVING_TO_TABLE:
                synchronized (stateLock) {
                    customerStats.decrementWaitingForTable();
                    customerStats.incrementWaitingForFood();
                    state = CustomerState.WAITING_FOR_WAITER;
                    notifyWaiter();
                }
                break;
            case LEAVING:
                synchronized (stateLock) {
                    customerStats.decrementEating();
                    entity.removeFromWorld();
                }
                break;
        }
    }

    private Entity findReceptionist() {
        for (Entity entity : entity.getWorld().getEntitiesByComponent(Receptionist.class)) {
            return entity;
        }
        return null;
    }

    public void assignTable(int tableNumber) {
        synchronized (stateLock) {
            this.tableNumber = tableNumber;
            state = CustomerState.MOVING_TO_TABLE;
            moveToTable();
        }
    }

    public void waitForTable() {
        synchronized (stateLock) {
            state = CustomerState.WAITING_FOR_TABLE;
        }
    }

    private void notifyWaiter() {
        customerQueueMonitor.addCustomer(this, tableNumber);
        state = CustomerState.WAITING_FOR_WAITER;
    }

    public void startEating() {
        synchronized (stateLock) {
            state = CustomerState.EATING;
            customerStats.decrementWaitingForFood();
            customerStats.incrementEating();

            new Thread(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextLong(
                            Config.MIN_EATING_TIME,
                            Config.MAX_EATING_TIME
                    ));

                    synchronized (stateLock) {
                        if (state == CustomerState.EATING) {
                            if (tableNumber != -1) {
                                restaurantMonitor.releaseTable(tableNumber);

                                for (Entity tableEntity : tables) {
                                    Table table = tableEntity.getComponent(Table.class);
                                    if (table != null && table.getNumber() == tableNumber) {
                                        table.release();
                                        break;
                                    }
                                }
                            }
                            state = CustomerState.LEAVING;
                            targetPosition = new Point2D(Config.ENTRANCE_X, Config.ENTRANCE_Y);
                            isMoving = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void moveToReceptionist() {
        synchronized (stateLock) {
            state = CustomerState.ENTERING;
            targetPosition = new Point2D(
                    Config.RECEPTIONIST_X - Config.SPRITE_SIZE * 2,
                    Config.RECEPTIONIST_Y
            );
            isMoving = true;
        }
    }

    private void moveToTable() {
        targetPosition = calculateTablePosition(tableNumber);
        isMoving = true;

        for (Entity tableEntity : tables) {
            Table table = tableEntity.getComponent(Table.class);
            if (table != null && table.getNumber() == tableNumber) {
                table.setCurrentCustomer(this);
                break;
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

    public int getId() {
        return id;
    }

    public CustomerState getState() {
        synchronized (stateLock) {
            return state;
        }
    }
}