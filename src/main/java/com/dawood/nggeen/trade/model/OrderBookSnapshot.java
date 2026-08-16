package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import com.dawood.nggeen.trade.model.enums.OrderType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class OrderBookSnapshot {
    private String symbol;
    private long sequenceNo;
    private long chronicleQueueIndex;
    private Instant createdAt;
    private List<RestingOrderState> bids;
    private List<RestingOrderState> asks;

    public static class RestingOrderState {
        private UUID orderId;
        private OrderSide side;
        private OrderType orderType;
        private BigDecimal price;
        private BigDecimal quantity;
        private BigDecimal filledQuantity;
        private BigDecimal remainingQuantity;
        private long sequenceNo;
        private OrderStatus status;
    }
}
