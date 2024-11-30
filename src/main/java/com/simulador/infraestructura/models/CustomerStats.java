package com.simulador.infraestructura.models;

import javafx.beans.property.SimpleIntegerProperty;

public class CustomerStats {
    private final SimpleIntegerProperty customersWaitingTable;
    private final SimpleIntegerProperty customersWaitingFood;
    private final SimpleIntegerProperty customersEating;
    private final SimpleIntegerProperty customersAtTables;

    public CustomerStats() {
        customersWaitingTable = new SimpleIntegerProperty(0);
        customersWaitingFood = new SimpleIntegerProperty(0);
        customersEating = new SimpleIntegerProperty(0);
        customersAtTables = new SimpleIntegerProperty(0);
    }

    public void incrementWaitingForTable() {
        customersWaitingTable.set(customersWaitingTable.get() + 1);
    }

    public void decrementWaitingForTable() {
        customersWaitingTable.set(customersWaitingTable.get() - 1);
        customersAtTables.set(customersAtTables.get() + 1);
    }

    public void incrementWaitingForFood() {
        customersWaitingFood.set(customersWaitingFood.get() + 1);
    }

    public void decrementWaitingForFood() {
        customersWaitingFood.set(customersWaitingFood.get() - 1);
    }

    public void incrementEating() {
        customersEating.set(customersEating.get() + 1);
    }

    public void decrementEating() {
        customersEating.set(customersEating.get() - 1);
        customersAtTables.set(customersAtTables.get() - 1);
    }

    public SimpleIntegerProperty customersWaitingTableProperty() {
        return customersWaitingTable;
    }

    public SimpleIntegerProperty customersWaitingFoodProperty() {
        return customersWaitingFood;
    }

    public SimpleIntegerProperty customersEatingProperty() {
        return customersEating;
    }

    public SimpleIntegerProperty customersAtTablesProperty() {
        return customersAtTables;
    }
}