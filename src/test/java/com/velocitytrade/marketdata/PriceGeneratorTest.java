package com.velocitytrade.marketdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceGeneratorTest {

    private PriceGenerator generator;
    private static final double INITIAL_PRICE = 100.0;
    private static final double VOLATILITY = 0.02;  // 2% annual volatility

    @BeforeEach
    void setup() {
        // Use fixed seed for reproducible tests
        generator = new PriceGenerator(12345L);
    }

    @Test
    void testPriceNeverNegative() {
        // Generate many prices to ensure none are negative
        for (int i = 0; i < 10000; i++) {
            double price = generator.nextPrice(INITIAL_PRICE, VOLATILITY);
            assertTrue(price > 0, "Price must be positive, got: " + price);
        }
    }

    @Test
    void testPriceWithinReasonableBounds() {
        // After 1000 updates (10 seconds at 100 Hz)
        // Price should stay within reasonable bounds
        double price = INITIAL_PRICE;

        for (int i = 0; i < 1000; i++) {
            price = generator.nextPrice(price, VOLATILITY);
        }

        // With 2% volatility, expect price within ±10% after 10 seconds
        // (3-sigma event is rare, this is ~2-sigma)
        double lowerBound = INITIAL_PRICE * 0.90;
        double upperBound = INITIAL_PRICE * 1.10;

        assertTrue(price >= lowerBound && price <= upperBound,
                String.format("Price %.2f outside bounds [%.2f, %.2f]",
                        price, lowerBound, upperBound));
    }

    @Test
    void testVolatilityImpact() {
        // Low volatility should produce smaller price movements
        PriceGenerator lowVolGen = new PriceGenerator(11111L);
        PriceGenerator highVolGen = new PriceGenerator(22222L);

        double lowVol = 0.01;   // 1% annual volatility
        double highVol = 0.04;  // 4% annual volatility

        // Generate 1000 prices and measure variance
        List<Double> lowVolPrices = new ArrayList<>();
        List<Double> highVolPrices = new ArrayList<>();

        double lowVolPrice = INITIAL_PRICE;
        double highVolPrice = INITIAL_PRICE;

        for (int i = 0; i < 1000; i++) {
            lowVolPrice = lowVolGen.nextPrice(lowVolPrice, lowVol);
            highVolPrice = highVolGen.nextPrice(highVolPrice, highVol);

            lowVolPrices.add(lowVolPrice);
            highVolPrices.add(highVolPrice);
        }

        double lowVolStdDev = calculateStdDev(lowVolPrices);
        double highVolStdDev = calculateStdDev(highVolPrices);

        // High volatility should produce larger standard deviation
        assertTrue(highVolStdDev > lowVolStdDev,
                String.format("High vol stddev (%.4f) should exceed low vol stddev (%.4f)",
                        highVolStdDev, lowVolStdDev));

        System.out.printf("Low vol (1%%) stddev: %.4f\n", lowVolStdDev);
        System.out.printf("High vol (4%%) stddev: %.4f\n", highVolStdDev);
    }

    @Test
    void testReproducibleWithSeed() {
        // Same seed should produce same sequence
        PriceGenerator gen1 = new PriceGenerator(99999L);
        PriceGenerator gen2 = new PriceGenerator(99999L);

        double price1 = INITIAL_PRICE;
        double price2 = INITIAL_PRICE;

        for (int i = 0; i < 100; i++) {
            price1 = gen1.nextPrice(price1, VOLATILITY);
            price2 = gen2.nextPrice(price2, VOLATILITY);

            assertEquals(price1, price2, 0.0001,
                    "Same seed should produce same prices");
        }
    }

    @Test
    void testBidAskSpreadValid() {
        // Generate quote and validate spread structure
        PriceQuote quote = generator.nextQuote(INITIAL_PRICE, VOLATILITY, 0.8);

        // Bid < Mid < Ask
        assertTrue(quote.bid() < quote.mid(), "Bid must be less than mid");
        assertTrue(quote.mid() < quote.ask(), "Mid must be less than ask");

        // Spread should be positive
        assertTrue(quote.spreadBps() > 0, "Spread must be positive");

        // All prices positive
        assertTrue(quote.bid() > 0,
                "All prices must be positive");

        // Validation method should pass
        assertTrue(quote.isValid(), "Quote should be valid");

        System.out.println("Quote: " + quote);
    }

    @Test
    void testLiquidityImpactsSpread() {
        // High liquidity should have tighter spread
        PriceQuote highLiqQuote = generator.nextQuote(INITIAL_PRICE, VOLATILITY, 0.95);
        PriceQuote lowLiqQuote = generator.nextQuote(INITIAL_PRICE, VOLATILITY, 0.30);

        assertTrue(highLiqQuote.spreadBps() < lowLiqQuote.spreadBps(),
                String.format("High liquidity spread (%.2f bps) should be tighter than low liquidity (%.2f bps)",
                        highLiqQuote.spreadBps(), lowLiqQuote.spreadBps()));

        System.out.printf("High liquidity (0.95): %.2f bps\n", highLiqQuote.spreadBps());
        System.out.printf("Low liquidity (0.30): %.2f bps\n", lowLiqQuote.spreadBps());
    }

    @Test
    void testIntradayVolatilityPatterns() {
        // Market open should have higher volatility than lunch
        // BUT: Due to randomness, we test by comparing averages over multiple runs

        PriceGenerator intradayGen = new PriceGenerator(55555L, 0.0, 0.01, 0.01, 0.0, 0.005, true);
        PriceGenerator noIntradayGen = new PriceGenerator(55556L, 0.0, 0.01, 0.01, 0.0, 0.005, false);

        LocalTime marketOpen = LocalTime.of(9, 35);   // 9:35 AM (should be 1.5x volatility)
        LocalTime lunchTime = LocalTime.of(13, 0);    // 1:00 PM (should be 0.7x volatility)

        // Run multiple trials to average out randomness
        int trials = 5;
        double[] openStdDevs = new double[trials];
        double[] lunchStdDevs = new double[trials];
        double[] noIntradayStdDevs = new double[trials];

        for (int trial = 0; trial < trials; trial++) {
            List<Double> openPrices = new ArrayList<>();
            List<Double> lunchPrices = new ArrayList<>();
            List<Double> noIntradayPrices = new ArrayList<>();

            double openPrice = INITIAL_PRICE;
            double lunchPrice = INITIAL_PRICE;
            double noIntradayPrice = INITIAL_PRICE;

            // Generate 1000 prices at each time (more samples = clearer signal)
            for (int i = 0; i < 1000; i++) {
                openPrice = intradayGen.nextPrice(openPrice, VOLATILITY, marketOpen);
                lunchPrice = intradayGen.nextPrice(lunchPrice, VOLATILITY, lunchTime);
                noIntradayPrice = noIntradayGen.nextPrice(noIntradayPrice, VOLATILITY, marketOpen);

                openPrices.add(openPrice);
                lunchPrices.add(lunchPrice);
                noIntradayPrices.add(noIntradayPrice);
            }

            openStdDevs[trial] = calculateStdDev(openPrices);
            lunchStdDevs[trial] = calculateStdDev(lunchPrices);
            noIntradayStdDevs[trial] = calculateStdDev(noIntradayPrices);
        }

        // Calculate average standard deviations across trials
        double avgOpenStdDev = Arrays.stream(openStdDevs).average().orElse(0.0);
        double avgLunchStdDev = Arrays.stream(lunchStdDevs).average().orElse(0.0);
        double avgNoIntradayStdDev = Arrays.stream(noIntradayStdDevs).average().orElse(0.0);

        System.out.printf("Market open (9:35 AM) avg stddev: %.4f (expected: ~1.5x base)\n", avgOpenStdDev);
        System.out.printf("Lunch time (1:00 PM) avg stddev: %.4f (expected: ~0.7x base)\n", avgLunchStdDev);
        System.out.printf("No intraday adjustment avg stddev: %.4f (base)\n", avgNoIntradayStdDev);

        // Test 1: Market open should have higher variance than lunch (averaged over trials)
        // With 1.5x vs 0.7x multiplier, we expect roughly 2x difference
        assertTrue(avgOpenStdDev > avgLunchStdDev,
                String.format("Market open variance (%.4f) should exceed lunch variance (%.4f)",
                        avgOpenStdDev, avgLunchStdDev));

        // Test 2: Intraday-enabled should differ from disabled (at market open)
        // Market open with 1.5x should be noticeably different from 1.0x
        double diffPercent = Math.abs(avgOpenStdDev - avgNoIntradayStdDev) / avgNoIntradayStdDev;
        assertTrue(diffPercent > 0.2,
                String.format("Intraday adjustment should create >20%% difference, got %.1f%%", diffPercent * 100));

        // Test 3: Verify the ratio is roughly what we expect
        // Market open = 1.5x base, Lunch = 0.7x base
        // Ratio should be approximately 1.5/0.7 ≈ 2.14
        double ratio = avgOpenStdDev / avgLunchStdDev;
        System.out.printf("Ratio (open/lunch): %.2f (expected: ~2.14)\n", ratio);

        // Allow 30% tolerance due to randomness
        assertTrue(ratio > 1.5 && ratio < 3.0,
                String.format("Ratio should be between 1.5-3.0, got %.2f", ratio));
    }


    @Test
    void testStatisticalProperties() {
        // Mean return should be close to drift (0.0)
        // Standard deviation should match volatility

        List<Double> returns = new ArrayList<>();
        double price = INITIAL_PRICE;

        for (int i = 0; i < 10000; i++) {
            double newPrice = generator.nextPrice(price, VOLATILITY);
            double returnPct = (newPrice - price) / price;
            returns.add(returnPct);
            price = newPrice;
        }

        double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDevReturn = calculateStdDev(returns);

        // Mean return should be close to 0 (drift = 0)
        assertTrue(Math.abs(meanReturn) < 0.001,
                "Mean return should be close to 0, got: " + meanReturn);

        // Standard deviation should be roughly volatility * sqrt(timeStep)
        // For 0.01s timeStep and 0.02 volatility: expected ~0.002
        double expectedStdDev = VOLATILITY * Math.sqrt(0.01);
        double tolerance = expectedStdDev * 0.3;  // 30% tolerance

        assertTrue(Math.abs(stdDevReturn - expectedStdDev) < tolerance,
                String.format("StdDev should be ~%.4f, got %.4f", expectedStdDev, stdDevReturn));

        System.out.printf("Mean return: %.6f (expected: 0.0)\n", meanReturn);
        System.out.printf("StdDev: %.4f (expected: %.4f)\n", stdDevReturn, expectedStdDev);
    }

    /**
     * Helper: Calculate standard deviation
     */
    private double calculateStdDev(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}
