package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderBookRegistry {
    private final Map<String, OrderBook> orderBooks = new HashMap<>();

    public void addOrderBook(Instrument instrument, OrderBook orderBook) {
        if (instrument == null) throw new IllegalArgumentException("Invalid Instrument");
        if (orderBook == null) throw new IllegalArgumentException("Invalid Order Book");
        orderBooks.put(instrument.getSymbol(), orderBook);
    }

    public OrderBook getByInstrument(String symbol) {
        if (symbol == null || symbol.isBlank()){
            throw new IllegalArgumentException("Invalid symbol");
        }

        if(!hasInstrument(symbol)){
            throw new IllegalArgumentException("Invalid Instrument");
        }

        return orderBooks.get(symbol);
    }

    private boolean hasInstrument(String symbol){
        return orderBooks.containsKey(symbol);
    }
}
