package com.velocitytrade.marketdata;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SymbolTest {

    @Test
    void testSymbolCreation() {
        Symbol aapl = new Symbol(0, "AAPL", "Apple Inc.", 195.50, 0.022);

        assertEquals(0, aapl.id());
        assertEquals("AAPL", aapl.ticker());
        assertEquals("Apple Inc.", aapl.name());
        assertEquals(195.50, aapl.initialPrice(), 0.01);
        assertEquals(0.022, aapl.volatility(), 0.0001);
    }

    @Test
    void testSymbolToString() {
        Symbol googl = new Symbol(1, "GOOGL", "Alphabet Inc.", 142.80, 0.0235);
        String str = googl.toString();

        assertTrue(str.contains("GOOGL"));
        assertTrue(str.contains("142.80"));
        assertTrue(str.contains("2.35%"));
    }

    @Test
    void testConvenienceConstructor() {
        Symbol symbol = new Symbol(2, "MSFT", 380.25, 0.0195);

        assertEquals("MSFT", symbol.ticker());
        assertEquals("MSFT", symbol.name());
    }
}
