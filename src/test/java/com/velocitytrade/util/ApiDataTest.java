package com.velocitytrade.util;

import com.velocitytrade.config.ConfigLoader;
import com.velocitytrade.config.ConfigLoader.AppConfig;
import com.velocitytrade.marketdata.Symbol;
import org.junit.jupiter.api.Test;

import java.util.List;

class ApiDataTest {

    @Test
    void testYahooFinanceApi() throws Exception {
        AppConfig config = ConfigLoader.loadConfig("config/application.yaml");

        // Create loader
        SymbolDataSource loader = SymbolLoaderFactory.createLoader(config);

        System.out.println("Data Source: " + loader.getSourceName());
        System.out.println("Fetching symbols (this takes ~20 seconds)...\n");

        // Fetch symbols
        List<Symbol> symbols = loader.loadSymbols();

        // Print first 10 symbols
        System.out.println("First 10 Symbols from Yahoo Finance");
        for (int i = 0; i < Math.min(10, symbols.size()); i++) {
            Symbol s = symbols.get(i);
            System.out.printf("%d. %s (%s): $%.2f, vol=%.2f%%\n",
                    i, s.ticker(),
                    s.name(),
                    s.initialPrice(),
                    s.volatility() * 100);
        }

        System.out.println("\nTotal symbols loaded: " + symbols.size());
    }
}
