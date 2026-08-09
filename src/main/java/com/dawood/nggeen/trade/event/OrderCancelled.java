package com.dawood.nggeen.trade.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelled(
        UUID orderId,
        long sequenceNo,
        String symbol,
        Instant timestamp,
        BigDecimal quantityCancelled


) implements DomainEvent {
}
