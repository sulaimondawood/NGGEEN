package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.trade.exception.InstrumentNotFound;
import com.dawood.nggeen.trade.exception.InvalidSymbolException;
import com.dawood.nggeen.trade.model.OrderBook;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Component
public class OrderBookRegistry {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();


    public void registerOrderBook(OrderBook orderBook) {
        if (orderBook == null || orderBook.getInstrument() == null) {
            throw new IllegalArgumentException("Invalid OrderBook or instrument symbol");
        }

        String symbol = orderBook.getInstrument();
        orderBooks.put(symbol, orderBook);

        ExecutorService executorService = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(10_000),
                threadFactory(symbol),
                new ThreadPoolExecutor.AbortPolicy()
        );

        executors.put(symbol, executorService);
    }

    public OrderBook getByInstrumentSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidSymbolException(ErrorCode.INVALID_SYMBOL, "Invalid symbol", HttpStatus.BAD_REQUEST);
        }

        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            throw new InstrumentNotFound(ErrorCode.NOT_FOUND, "Instrument not available: " + symbol, HttpStatus.NOT_FOUND);
        }

        return orderBook;
    }

    public ExecutorService getExecutorFor(String symbol) {
        ExecutorService executor = executors.get(symbol);
        if (executor == null) {
            throw new InstrumentNotFound(ErrorCode.NOT_FOUND, "No thread executor configured for instrument: " + symbol, HttpStatus.NOT_FOUND);
        }
        return executor;
    }

    @PreDestroy
    public void shutdown() {
        executors.values().forEach(executor -> {
            executor.shutdown();
            try {
                // Wait up to 5 seconds for pending matching tasks in queue to finish
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }

            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    public Map<String, OrderBook> getAllOrderBooks() {
        return this.orderBooks;
    }

    private ThreadFactory threadFactory(String symbol) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setName("engine-matching-" + symbol);
            thread.setDaemon(true);
            return thread;
        };
    }
}
