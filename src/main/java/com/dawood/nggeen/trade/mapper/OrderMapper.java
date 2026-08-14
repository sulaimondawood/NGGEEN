package com.dawood.nggeen.trade.mapper;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.model.Order;

import java.util.UUID;

public class OrderMapper {
    public static Order toDomainOrder(PlaceOrderRequest orderRequest, UUID id){
        return Order.builder()
                .id(id)
                .symbol(orderRequest.getSymbol())
                .orderType(orderRequest.getOrderType())
                .orderSide(orderRequest.getOrderSide())
                .price(orderRequest.getPrice())
                .stopPrice(orderRequest.getStopPrice())
                .quantity(orderRequest.getQuantity())
                .remainingQuantity(orderRequest.getQuantity())
                .build();
    }
}
