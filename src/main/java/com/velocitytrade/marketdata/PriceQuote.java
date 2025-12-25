package com.velocitytrade.marketdata;

import org.jetbrains.annotations.NotNull;

public record PriceQuote (
        double mid,
        double bid,
        double ask,
        double spreadBps,
        long timestamp
) {
    public PriceQuote(double mid, double bid, double ask, double spreadBps) {
        this(mid, bid, ask, spreadBps, System.nanoTime());
    }

    public double spreadPercent() {
        return (ask - bid) / mid * 100.0;
    }

    public boolean isValid() {
        return bid > 0 && bid < mid && mid < ask;
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("Quote[mid=%.2f, bid=%.2f, ask=%.2f, spread=%.1fbps]",
                mid, bid, ask, spreadBps);
    }
}
