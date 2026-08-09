package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeExecuted(
        long sequenceNo,
        UUID tradeId,
        UUID buyOrderId,
        UUID sellOrderId,
        String symbol,
        Instant timestamp,
        BigDecimal price,
        BigDecimal quantity,
        OrderSide aggressorSide
) implements DomainEvent{
    public TradeExecuted(  long sequenceNo,
                           UUID tradeId,
                           UUID buyOrderId,
                           UUID sellOrderId,
                           String symbol,
                           BigDecimal price,
                           BigDecimal quantity, OrderSide aggressorSide){
        this(sequenceNo,tradeId,buyOrderId,sellOrderId,symbol,Instant.now(),price,quantity, aggressorSide);
    }
}
