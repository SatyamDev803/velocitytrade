package com.velocitytrade.marketdata;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.Random;

@Slf4j
public class PriceGenerator {
    private final Random random;
    private final double drift;
    private final double timeStep;
    private final double jumpProbability;
    private final double jumpMean;
    private final double jumpStdDev;
    private final boolean enableIntradayVolatility;


    public PriceGenerator(long seed) {
        this(seed, 0.0, 0.01, 0.01, 0.0, 0.005, true);
    }

    public PriceGenerator(long seed, double drift, double timeStep, double jumpProbability, double jumpMean, double jumpStdDev, boolean enableIntradayVolatility) {
        this.random = new Random(seed);
        this.drift = drift;
        this.timeStep = timeStep;
        this.jumpProbability = jumpProbability;
        this.jumpMean = jumpMean;
        this.jumpStdDev = jumpStdDev;
        this.enableIntradayVolatility = enableIntradayVolatility;

        log.debug("PriceGenerator created: drift={}, timeStep={}, jumpProb={}", drift, timeStep, jumpProbability);
    }

    public double nextPrice(double currentPrice, double volatility) {
        return nextPrice(currentPrice, volatility, LocalTime.now());
    }

    public double nextPrice(double currentPrice, double volatility, LocalTime time) {

        // Adjust volatility based on time of day
        double adjustedVal = enableIntradayVolatility
                ? adjustVolatilityByTime(volatility, time)
                : volatility;

        // Geometric Brownian Motion component
        double z = random.nextGaussian();
        double diffusion = (drift - 0.5 * adjustedVal * adjustedVal) * timeStep + adjustedVal * Math.sqrt(timeStep) * z;

        // Jump component - Merton model
        double jump = 0.0;
        if(random.nextDouble() < jumpProbability) {
            jump = jumpMean + jumpStdDev * random.nextGaussian();
            log.trace("Jump event: size={:.3f}%", jump * 100);
        }

        // combined price evolution
        double newPrice = currentPrice * Math.exp(diffusion + jump);

        // Price stays positive
        return Math.max(newPrice, 0.01);
    }

    public PriceQuote nextQuote(double currentPrice, double volatility, double liquidity) {
        return nextQuote(currentPrice, volatility, liquidity, LocalTime.now());
    }

    public PriceQuote nextQuote(double currentPrice, double volatility, double liquidity, LocalTime time) {

        // mid-price
        double midPrice = nextPrice(currentPrice, volatility, time);

        // calculate spread based on liquidity
        double spreadBps = calculateSpreadBps(liquidity, volatility);

        double halfSpread = midPrice * spreadBps / 20000.0;

        double bid = midPrice - halfSpread;
        double ask = midPrice + halfSpread;

        return new PriceQuote(midPrice, bid, ask, spreadBps);
    }

    private double calculateSpreadBps(double liquidity, double volatility) {
        double liquiditySpread = 1.0 + (1.0 - liquidity) * 99.0;

        double volMultiplier = 1.0 + (volatility / 0.02 - 1.0) * 0.3;

        return liquiditySpread * volMultiplier;
    }

    private double adjustVolatilityByTime(double baseVolatility, LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        int totalMinutes = hour * 60 + minute;

        if(totalMinutes >= 570 && totalMinutes < 600) {
            return baseVolatility * 1.5;
        }

        if(totalMinutes >= 600 && totalMinutes < 720) {
            return baseVolatility;
        }

        if(totalMinutes >= 720 && totalMinutes < 840) {
            return baseVolatility * 0.7;
        }

        if(totalMinutes >= 840 && totalMinutes < 930) {
            return baseVolatility;
        }

        if(totalMinutes >= 930 && totalMinutes < 960) {
            return baseVolatility * 1.3;
        }

        return baseVolatility;
    }

    public static double estimateLiquidity(Symbol symbol) {
        if(symbol.id() < 10) {
            return 0.95 + LIQUIDITY_RANDOM.nextDouble() * 0.05;
        }

        if(symbol.id() < 50) {
            return 0.70 + LIQUIDITY_RANDOM.nextDouble() * 0.20;
        }

        return 0.40 + LIQUIDITY_RANDOM.nextDouble() * 0.30;
    }

    private static final Random LIQUIDITY_RANDOM = new Random();
}
