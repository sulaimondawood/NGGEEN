package com.dawood.nggeen.trade.mapper;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.event.OrderAccepted;
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

    public static Order fromEvent(OrderAccepted event){
        return Order.builder()
                .id(event.getOrderId())
                .symbol(event.symbol())
                .orderType(event.getOrderType())
                .orderSide(event.getOrderSide())
                .price(event.getPrice())
                .stopPrice(event.getStopPrice())
                .quantity(event.getQuantity())
                .build();
    }
}
