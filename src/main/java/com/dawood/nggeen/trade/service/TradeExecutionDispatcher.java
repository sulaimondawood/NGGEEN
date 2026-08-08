package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class TradeExecutionDispatcher {
    private final OrderBookRegistry orderBookRegistry;

    public void dispatch(OrderBook instrumentOrderBook, Order incomingOrder) {
        String symbol = incomingOrder.getSymbol();
        ExecutorService executor = orderBookRegistry.getExecutorFor(symbol);

        executor.submit(() -> {
            try {
                long seq = instrumentOrderBook.getSequenceGenerator().next();
                incomingOrder.setSequenceNo(seq);
                instrumentOrderBook.processOrder(incomingOrder);
            } catch (Exception e) {
                System.err.printf("Error matching order %s on %s: %s%n",
                        incomingOrder.getId(), symbol, e.getMessage());
            }
        });

    }

}
