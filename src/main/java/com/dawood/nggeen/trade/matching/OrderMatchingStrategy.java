package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;

public interface OrderMatchingStrategy {
    void match(Order incomingOrder, OrderBook orderBook);
}
