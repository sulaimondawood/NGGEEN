package com.dawood.nggeen.trade.mapper;

import com.dawood.nggeen.trade.api.rest.dto.OrderResponse;
import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.model.Order;

import java.util.UUID;

public class OrderMapper {
    public static Order toDomainOrder(PlaceOrderRequest orderRequest, UUID id) {
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

    public static Order fromEvent(OrderAccepted event) {
        return Order.builder()
                .id(event.getOrderId())
                .accountId(event.getAccountId())
                .userId(event.getUserId())
                .symbol(event.symbol())
                .baseAsset(event.getBaseAsset())
                .quoteAsset(event.getQuoteAsset())
                .price(event.getPrice())
                .stopPrice(event.getStopPrice())
                .quantity(event.getQuantity())
                .orderType(event.getOrderType())
                .orderSide(event.getOrderSide())
                .remainingQuantity(event.getQuantity())
                .build();
    }

    public static OrderResponse toDTO(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSymbol(),
                order.getOrderType(),
                order.getOrderSide(),
                order.getPrice(),
                order.getStopPrice(),
                order.getQuantity()
        );
    }
}
