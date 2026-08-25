package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderType;
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
public class OrderAccepted implements DomainEvent {
    private UUID orderId;
    private long sequenceNo;
    private UUID accountId;
    private UUID userId;
    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private Instant timestamp;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private BigDecimal quantity;
    private OrderSide orderSide;
    private OrderType orderType;

    public OrderAccepted(UUID orderId,
                         long sequenceNo,
                         UUID accountId,
                         UUID userId,
                         String symbol,
                         String baseAsset,
                         String quoteAsset,
                         BigDecimal price,
                         BigDecimal stopPrice,
                         BigDecimal quantity,
                         OrderSide orderSide,
                         OrderType orderType) {
        this(orderId, sequenceNo, accountId, userId, symbol, baseAsset, quoteAsset, Instant.now(), price, stopPrice, quantity, orderSide, orderType);
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