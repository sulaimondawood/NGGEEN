package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final Map<String, OrderMatchingStrategy> orderMatchingStrategies;

    public void processIncomingOrder(PlaceOrderRequest orderRequest){
//        orderRequest.get

    }

}
