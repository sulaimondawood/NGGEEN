package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public record OrderAccepted(
        UUID orderId,
        long sequenceNo,
        String symbol,
        Instant timestamp,
        BigDecimal price,
        BigDecimal stopPrice,
        BigDecimal quantity,
        OrderSide orderSide,
        OrderType orderType
) implements DomainEvent {

    public OrderAccepted(UUID orderId, long sequenceNo, String symbol,
                         BigDecimal price, BigDecimal stopPrice, BigDecimal quantity,
                         OrderSide orderSide, OrderType orderType) {
        this(orderId, sequenceNo, symbol, Instant.now(), price, stopPrice, quantity, orderSide, orderType);
    }

}
