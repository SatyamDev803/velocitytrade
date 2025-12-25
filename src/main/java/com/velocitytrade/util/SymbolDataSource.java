package com.velocitytrade.util;

import com.velocitytrade.marketdata.Symbol;
import java.util.List;

public interface SymbolDataSource {
    List<Symbol> loadSymbols();

    String getSourceName();
}
