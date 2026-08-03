package com.dawood.nggeen.trade.mapper;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.model.Order;

public class OrderMapper {
    public static Order toDomainOrder(PlaceOrderRequest orderRequest){
        return Order.builder()
                .instrument(null)
                .sequenceNo(1)
                .orderType(orderRequest.getOrderType())
                .orderSide(orderRequest.getOrderSide())
                .price(orderRequest.getPrice())
                .stopPrice(orderRequest.getStopPrice())
                .quantity(orderRequest.getQuantity())
                .remainingQuantity(orderRequest.getQuantity())
                .build();
    }
}
