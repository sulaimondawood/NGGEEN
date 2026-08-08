package com.dawood.nggeen.trade.event;


import com.dawood.nggeen.trade.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeExecuted implements DomainEvent(
long sequenceNo,
String instrumentId,
UUID tradeId,
BigDecimal price,
BigDecimal quantity,
UUID buyOrderId,
UUID sellOrderId,
OrderSide aggressorSide,
Instant timestamp
        ){
                }
