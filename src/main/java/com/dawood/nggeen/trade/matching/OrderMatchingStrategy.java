package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;

public interface OrderMatchingStrategy {
    void match(PlaceOrderRequest orderRequest);
}
