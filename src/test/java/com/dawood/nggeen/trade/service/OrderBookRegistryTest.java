package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookRegistryTest {

    private OrderBookRegistry registry;
    private final String symbol = "BTCUSDT";

    @BeforeEach
    void setup(){
        registry = new OrderBookRegistry();
    }

    @Test
    void shouldRegister_And_RetrieveOrderBook_AndExecutorThread(){
        OrderBook orderBook = new OrderBook(Map.of());
        orderBook.setInstrument(symbol);

        Instrument instrument = new Instrument();

        registry.registerOrderBook(orderBook, instrument);

        assertEquals(orderBook, registry.getByInstrumentSymbol(symbol));
        assertNotNull(registry.getExecutorFor(symbol));
        assertTrue(registry.getAllOrderBooks().containsKey(symbol));
    }

    @Test
    void shouldThrowException_WhenRegisteringInvalidOrderBook(){
       IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,()->registry.registerOrderBook(null, null));

       OrderBook orderBook = new OrderBook(Map.of());
       IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,()->registry.registerOrderBook(orderBook, null));

       assertEquals("Invalid OrderBook or instrument symbol", exception.getMessage());
       assertEquals("Invalid OrderBook or instrument symbol", exception2.getMessage());
    }

    @Test
    void shouldGetInstrumentOrderBook_WhenSymbolValueIsValid(){
        OrderBook orderBook = new OrderBook(Map.of());
        orderBook.setInstrument(symbol);
        Instrument instrument = new Instrument();

        registry.registerOrderBook(orderBook, instrument);

        registry.getByInstrumentSymbol(symbol);

        assertEquals(orderBook, registry.getByInstrumentSymbol(symbol));
    }


    @Test
    void shouldShutdownAllExecutors() {
        OrderBook orderBook = new OrderBook(Map.of());
        orderBook.setInstrument(symbol);

        OrderBook orderBook2 = new OrderBook(Map.of());
        orderBook2.setInstrument("ETHUSDT");



        registry.registerOrderBook(orderBook, new Instrument());
        registry.registerOrderBook(orderBook2, new Instrument());

        ExecutorService btcExecutor = registry.getExecutorFor(symbol);
        ExecutorService ethExecutor = registry.getExecutorFor("ETHUSDT");

        registry.shutdown();

        assertTrue(btcExecutor.isShutdown());
        assertTrue(ethExecutor.isShutdown());
    }
}