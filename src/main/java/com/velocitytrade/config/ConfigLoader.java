package com.velocitytrade.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static AppConfig loadConfig(String filepath) throws IOException {
        return mapper.readValue(new File(filepath), AppConfig.class);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppConfig {
        private SystemConfig system;
        private MarketDataConfig market_data;
        private TradingConfig trading;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemConfig {
        private String name;
        private String version;
        private String environment;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MarketDataConfig {
        private String data_source;
        private CsvConfig csv;
        private ApiConfig api;
        private boolean enabled;
        private String multicast_group;
        private int multicast_port;
        private int update_frequency_hz;
        private LoggingConfig logging;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CsvConfig {
        private String filepath;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiConfig {
        private String provider;
        private String base_url;
        private int timeout_seconds;
        private int cache_ttl_hours;
        private boolean fallback_to_csv;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoggingConfig {
        private int log_every_n_messages;
        private int detailed_report_interval_sec;
        private boolean log_individual_messages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TradingConfig {
        private boolean enabled;
    }
}
