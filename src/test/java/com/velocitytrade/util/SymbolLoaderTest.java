package com.velocitytrade.util;

import com.velocitytrade.marketdata.Symbol;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SymbolLoaderTest {

    @Test
    void testCsvLoader() {
        CsvSymbolLoader loader = new CsvSymbolLoader("config/sp100.csv");
        List<Symbol> symbols = loader.loadSymbols();

        assertEquals(100, symbols.size());
        assertEquals("AAPL", symbols.get(0).ticker());
        assertTrue(symbols.get(0).initialPrice() > 0);
    }
}
