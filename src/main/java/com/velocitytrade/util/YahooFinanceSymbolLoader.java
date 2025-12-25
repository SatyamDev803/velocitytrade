package com.velocitytrade.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitytrade.marketdata.Symbol;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class YahooFinanceSymbolLoader implements SymbolDataSource{

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SymbolDataSource fallbackLoader;

    public YahooFinanceSymbolLoader(String baseUrl, int timeoutSeconds, String fallbackCsvPath) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.fallbackLoader = new CsvSymbolLoader(fallbackCsvPath);
    }

    @Override
    public List<Symbol> loadSymbols() {
        log.info("Fetching live data from Yahoo Finance API..");

        List<Symbol> csvSymbols = fallbackLoader.loadSymbols();
        List<Symbol> apiSymbols = new ArrayList<>();

        int successCount = 0;
        int failCount = 0;

        for (Symbol csvSymbol : csvSymbols) {
            try {
                double currentPrice = fetchCurrentPrice(csvSymbol.ticker());

                double volatility = csvSymbol.volatility();

                apiSymbols.add(new Symbol(
                        csvSymbol.id(),
                        csvSymbol.ticker(),
                        csvSymbol.name(),
                        currentPrice,
                        volatility
                ));

                successCount++;

                if(successCount % 5 == 0) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                log.warn("Failed to fetch data for {}, using CSV fallback", csvSymbol.ticker());
                apiSymbols.add(csvSymbol);
                failCount++;
            }
        }

        log.info("Loaded {} symbols from Yahoo Finance ({} success, {} fallback)", apiSymbols.size(), successCount, failCount);

        return apiSymbols;
    }

    private double fetchCurrentPrice(String ticker) throws IOException {
        String url = String.format("%s/v8/finance/chart/%s?interval=1d&range=1d", baseUrl, ticker);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("user-agent", "Mozilla/5.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed: " + response.code());
            }

            assert response.body() != null;
            String jsonString = response.body().string();
            JsonNode root = objectMapper.readTree(jsonString);

            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode meta = result.path("meta");
            double price = meta.path("regularMarketPrice").asDouble();

            if (price == 0.0) {
                throw new IOException("Invalid price from API");
            }

            return price;
        }
    }

    @Override
    public String getSourceName() {
        return "Yahoo Finance API";
    }
}
