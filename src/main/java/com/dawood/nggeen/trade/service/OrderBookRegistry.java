package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.OrderBook;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderBookRegistry {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    public void registerOrderBook(OrderBook orderBook) {
        if (orderBook == null || orderBook.getInstrument() == null) {
            throw new IllegalArgumentException("Invalid OrderBook or instrument symbol");
        }
        orderBooks.put(orderBook.getInstrument(), orderBook);
    }

    public OrderBook getByInstrumentSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Invalid symbol");
        }

        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            throw new IllegalArgumentException("Instrument not available: " + symbol);
        }

        return orderBook;
    }

    public boolean hasInstrument(String symbol) {
        return orderBooks.containsKey(symbol);
    }
}
