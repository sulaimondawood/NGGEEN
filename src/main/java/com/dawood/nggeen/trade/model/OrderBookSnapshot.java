package com.dawood.nggeen.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class OrderBookSnapshot {
    private String symbol;
    private long sequenceNo;
    private Instant createdAt;
    private List<Order> bids;
    private long chronicleQueueIndex;
    private List<Order> asks;
}
