package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import org.springframework.stereotype.Component;

@Component(value = "limit")
public class LimitOrderMatching implements OrderMatchingStrategy {
    @Override
    public void match(PlaceOrderRequest orderRequest) {

    }
}
