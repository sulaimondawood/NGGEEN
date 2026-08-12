package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.model.enums.OrderSide;
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
public class TradeExecuted implements DomainEvent {
    private long sequenceNo;
    private UUID tradeId;
    private UUID buyOrderId;
    private UUID sellOrderId;
    private String symbol;
    private Instant timestamp;
    private BigDecimal price;
    private BigDecimal executedQuantity;
    private OrderSide aggressorSide;

    public TradeExecuted(long sequenceNo,
                         UUID tradeId,
                         UUID buyOrderId,
                         UUID sellOrderId,
                         String symbol,
                         BigDecimal price,
                         BigDecimal executedQuantity,
                         OrderSide aggressorSide) {
        this(sequenceNo, tradeId, buyOrderId, sellOrderId, symbol, Instant.now(), price, executedQuantity, aggressorSide);
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