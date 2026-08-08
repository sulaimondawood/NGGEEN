package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class TradeExecutionDispatcher {
    private final OrderBookRegistry orderBookRegistry;

    public void dispatch(OrderBook instrumentOrderBook, Order incomingOrder){
        instrumentOrderBook.processOrder(incomingOrder);

    }

}
