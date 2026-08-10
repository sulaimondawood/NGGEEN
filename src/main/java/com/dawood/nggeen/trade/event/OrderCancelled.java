package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.model.enums.CancelReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelled(
        UUID orderId,
        long sequenceNo,
        String symbol,
        Instant timestamp,
        BigDecimal quantityCancelled,
        CancelReason reason
) implements DomainEvent {
    public OrderCancelled( UUID orderId,
                           long sequenceNo,
                           String symbol,
                           BigDecimal quantityCancelled,
                           CancelReason reason){
        this(orderId, sequenceNo, symbol, Instant.now(), quantityCancelled, reason);
    }
}
