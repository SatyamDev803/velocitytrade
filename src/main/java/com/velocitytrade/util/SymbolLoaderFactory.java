package com.velocitytrade.util;

import com.velocitytrade.config.ConfigLoader.AppConfig;
import com.velocitytrade.config.ConfigLoader.MarketDataConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SymbolLoaderFactory {

    public static SymbolDataSource createLoader(AppConfig config) {
        MarketDataConfig marketDataConfig = config.getMarket_data();
        String dataSource = marketDataConfig.getData_source();

        log.info("Creating symbol loader: {}", dataSource);

        if ("api".equalsIgnoreCase(dataSource)) {
            return createApiLoader(marketDataConfig);
        } else {
            return createCsvLoader(marketDataConfig);
        }
    }

    private static SymbolDataSource createCsvLoader(MarketDataConfig config) {
        String filepath = config.getCsv().getFilepath();
        return new CsvSymbolLoader(filepath);
    }

    private static SymbolDataSource createApiLoader(MarketDataConfig config) {
        var apiConfig = config.getApi();
        var csvConfig = config.getCsv();

        String baseUrl = apiConfig.getBase_url();
        int timeout = apiConfig.getTimeout_seconds();
        String fallbackCsv = csvConfig.getFilepath();

        return new YahooFinanceSymbolLoader(baseUrl, timeout, fallbackCsv);
    }
}
