package com.dawood.nggeen.trade.mapper;

import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.model.Trade;

public class TradeMapper {
    public static Trade fromEvent(TradeExecuted event){
    return Trade.builder()
                .id(event.getTradeId())
                .symbol(event.symbol())
                .sellOrderId(event.getSellOrderId())
                .buyOrderId(event.getBuyOrderId())
                .price(event.getPrice())
                .quantity(event.getExecutedQuantity())
                .executedAt(event.getTimestamp())
                .build();


    }
}
