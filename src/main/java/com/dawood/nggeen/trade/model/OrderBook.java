package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeMap;

@Getter
public class OrderBook {
    private OrderMatchingStrategy orderMatchingStrategy;

    private String instrument;
    private TreeMap<BigDecimal, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();

    public OrderBook(OrderMatchingStrategy strategy) {
        this.orderMatchingStrategy = strategy;
    }

    public void processOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) throw new IllegalArgumentException("Invalid order request");
        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest);
        orderMatchingStrategy.match(incomingOrder, this);


    }

    public BigDecimal getBestBidOrOffer(OrderSide orderSide) {
        return orderSide == OrderSide.BUY ? getBestAsk() : getBestBid();
    }

    private BigDecimal getBestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    private BigDecimal getBestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    public TreeMap<BigDecimal, LinkedList<Order>> oppositeOrderBookSide(OrderSide orderSide) {
        return orderSide == OrderSide.BUY ? asks : bids;
    }

    public void addOrderToBook(Order incomingOrder) {
        OrderSide orderSide = incomingOrder.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> orderBookSide = orderSide == OrderSide.BUY ? bids : asks;
        orderBookSide.computeIfAbsent(incomingOrder.getPrice(), (p) -> new LinkedList<>())
                .addLast(incomingOrder);


    }
}
