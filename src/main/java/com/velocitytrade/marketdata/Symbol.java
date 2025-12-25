package com.velocitytrade.marketdata;


import org.jetbrains.annotations.NotNull;

public record Symbol(int id, String ticker, String name, double initialPrice, double volatility) {
    public Symbol(int id, String ticker, double initialPrice, double volatility) {
        this(id, ticker, ticker, initialPrice, volatility);
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("%s (%s): $%.2f, vol=%.2f%%", ticker, name, initialPrice, volatility * 100);
    }
}
