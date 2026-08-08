package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import com.dawood.nggeen.trade.service.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class OrderBook {
    private Map<String, OrderMatchingStrategy> matchingStrategies;
    private SequenceGenerator sequenceGenerator = new SequenceGenerator();

    private String instrument;
    private TreeMap<BigDecimal, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();
    private Map<UUID, Order> orderMap = new ConcurrentHashMap<>();

    public OrderBook(Map<String, OrderMatchingStrategy> strategy) {
        this.matchingStrategies = strategy;
    }

    public void processOrder(Order incomingOrder) {
        if (incomingOrder == null) throw new IllegalArgumentException("Invalid order");

        OrderMatchingStrategy matchingStrategy = matchingStrategies.get(incomingOrder.getOrderType().name().toUpperCase());
        if (matchingStrategy == null) {
            throw new IllegalArgumentException("No matcher for order type: " + incomingOrder.getOrderType());
        }

        matchingStrategy.match(incomingOrder, this);
    }

    public BigDecimal getBestBidOrOffer(OrderSide orderSide) {
        return orderSide == OrderSide.BUY ? getBestAsk() : getBestBid();
    }

    public void addOrderToBook(Order incomingOrder) {
        if (incomingOrder == null) throw new IllegalArgumentException("Invalid order");

        OrderSide orderSide = incomingOrder.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> orderBookSide = orderSide == OrderSide.BUY ? bids : asks;
        orderBookSide.computeIfAbsent(incomingOrder.getPrice(), (p) -> new LinkedList<>())
                .addLast(incomingOrder);

        if (incomingOrder.getId() != null) {
            orderMap.put(incomingOrder.getId(), incomingOrder);
        }

    }

    public void cancelOrder(UUID orderId) {
        if (orderId == null) throw new IllegalArgumentException("Invalid order id");

        Order pendingOrder = orderMap.get(orderId);
        if (pendingOrder == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (pendingOrder.isFilled()) {
            throw new IllegalArgumentException("Order has already been filled");
        }

        TreeMap<BigDecimal, LinkedList<Order>> orderBookSide =
                pendingOrder.getOrderSide() == OrderSide.BUY ? bids : asks;

        LinkedList<Order> restingOrders = orderBookSide.get(pendingOrder.getPrice());
        if (restingOrders == null || !restingOrders.remove(pendingOrder)) {
            throw new IllegalArgumentException("Order is no longer active in order book");
        }

        if (restingOrders.isEmpty()) {
            orderBookSide.remove(pendingOrder.getPrice());
        }

        pendingOrder.setStatus(OrderStatus.CANCELED);
        orderMap.remove(orderId);
    }

    public TreeMap<BigDecimal, LinkedList<Order>> oppositeOrderBookSide(OrderSide orderSide) {
        return orderSide == OrderSide.BUY ? asks : bids;
    }

    private BigDecimal getBestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    private BigDecimal getBestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

}
