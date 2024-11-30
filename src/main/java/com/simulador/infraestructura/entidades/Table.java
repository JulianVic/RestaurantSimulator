package com.simulador.infraestructura.entidades;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

public class Table extends Component {
    private final int number;
    private final Point2D position;
    private boolean isOccupied;
    private Customer currentCustomer;

    public Table(int number, Point2D position) {
        this.number = number;
        this.position = position;
        this.isOccupied = false;
        this.currentCustomer = null;
    }

    @Override
    public void onUpdate(double tpf) {
        isOccupied = (currentCustomer != null);
    }

    public int getNumber() {
        return number;
    }

    public void release() {
        this.currentCustomer = null;
        this.isOccupied = false;
    }

    public Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
        this.isOccupied = (customer != null);
    }
}