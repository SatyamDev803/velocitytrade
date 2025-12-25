package com.velocitytrade.util;

import com.velocitytrade.marketdata.Symbol;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CsvSymbolLoader implements SymbolDataSource {
    private final String filepath;

    public CsvSymbolLoader (String filepath) {
        this.filepath = filepath;
    }

    @Override
    public List<Symbol> loadSymbols() {
        log.info("Loading symbols from CSV: {}", filepath);

        try (var lines = Files.lines(Paths.get(filepath))) {
            List<Symbol> symbols = lines
                    .skip(1)
                    .map(this::parseSymbol)
                    .collect(Collectors.toList())
                    ;

            log.info("Loaded {} symbols from CSV", symbols.size());
            return symbols;
        } catch (IOException e) {
            log.error("Failed to load symbols from CSV: {}", filepath, e);
            throw new RuntimeException("Failed to load symbols from CSV", e);
        }
    }

    private Symbol parseSymbol(String line) {
        String[] parts = line.split(",");
        if(parts.length < 5) {
            throw new IllegalArgumentException("Invalid CSV line: "+line);
        }

        return new Symbol(
                Integer.parseInt(parts[0].trim()),  // id
                parts[1].trim(),  // ticker
                parts[2].trim(),  // name
                Double.parseDouble(parts[3].trim()),  // price
                Double.parseDouble(parts[4].trim())  // volatility
        );
    }

    @Override
    public String getSourceName() {
        return "CSV (" + filepath + ")";
    }
}
