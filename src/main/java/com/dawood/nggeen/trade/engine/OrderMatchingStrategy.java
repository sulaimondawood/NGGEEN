package com.dawood.nggeen.trade.engine;

import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;

public interface OrderMatchingStrategy {
    void match(Order incomingOrder, OrderBook orderBook);
}
