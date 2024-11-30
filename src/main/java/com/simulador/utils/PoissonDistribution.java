package com.simulador.utils;

public class PoissonDistribution {
    private final double lambda;

    public PoissonDistribution(double lambda) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("Lambda must be greater than 0");
        }
        this.lambda = lambda;
    }

    public int nextInt() {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= Math.random();
        } while (p > L);

        return k - 1;
    }

    public double nextArrivalTime() {
        return -Math.log(1.0 - Math.random()) / lambda;
    }
}