package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.model.enums.CancelReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelled implements DomainEvent {
    private UUID orderId;
    private long sequenceNo;
    private String symbol;
    private Instant timestamp;
    private BigDecimal quantityCancelled;
    private CancelReason reason;

    public OrderCancelled(UUID orderId,
                          long sequenceNo,
                          String symbol,
                          BigDecimal quantityCancelled,
                          CancelReason reason) {
        this(orderId, sequenceNo, symbol, Instant.now(), quantityCancelled, reason);
    }

    @Override
    public long sequenceNo() {
        return this.sequenceNo;
    }

    @Override
    public String symbol() {
        return this.symbol;
    }

    @Override
    public Instant timestamp() {
        return this.timestamp;
    }
}