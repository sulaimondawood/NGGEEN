package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
public class OrderBook {
    private Map<String, OrderMatchingStrategy> matchingStrategies;

    private String instrument;
    private TreeMap<BigDecimal, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();

    public OrderBook(Map<String, OrderMatchingStrategy> strategy) {
        this.matchingStrategies = strategy;
    }

    public void processOrder(Order incomingOrder) {
        if (incomingOrder == null) throw new IllegalArgumentException("Invalid order");

        OrderMatchingStrategy matchingStrategy = matchingStrategies.get(incomingOrder.getOrderType().name().toUpperCase());
        if(matchingStrategy == null) {
            throw new IllegalArgumentException("No matcher for order type: " + incomingOrder.getOrderType());
        }

        matchingStrategy.match(incomingOrder, this);
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
        if(incomingOrder == null)  throw new IllegalArgumentException("Invalid order");

        OrderSide orderSide = incomingOrder.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> orderBookSide = orderSide == OrderSide.BUY ? bids : asks;
        orderBookSide.computeIfAbsent(incomingOrder.getPrice(), (p) -> new LinkedList<>())
                .addLast(incomingOrder);


    }
}
