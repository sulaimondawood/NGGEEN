package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.OrderBook;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderBookRegistry {
    private final Map<String, OrderBook> orderBooks = new HashMap<>();

    public void registerOrderBook(OrderBook orderBook) {
        orderBooks.put(orderBook.getInstrument(), orderBook);
    }

    public OrderBook getByInstrumentSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Invalid symbol");
        }

        if (!hasInstrument(symbol)) {
            throw new IllegalArgumentException("Instrument not available");
        }

        return orderBooks.get(symbol);
    }

    private boolean hasInstrument(String symbol) {
        return orderBooks.containsKey(symbol);
    }
}
