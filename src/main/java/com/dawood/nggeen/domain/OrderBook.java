package com.dawood.nggeen.domain;

import com.dawood.nggeen.domain.enums.OrderStatus;
import com.dawood.nggeen.domain.enums.OrderType;
import com.dawood.nggeen.domain.enums.Side;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
public class OrderBook {
    private TreeMap<BigDecimal, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());

    private TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();

    private Map<UUID, Order> orderMap = new HashMap<>();

    public void processOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Invalid order");
        }

        Side side = order.getSide();
        OrderType type = order.getType();

        if (side.equals(Side.BUY)) {
            matchOrder(order, asks, true);
        } else {
            matchOrder(order, bids, false);
        }

        if (!order.isFilled()) {
            addToOrderTreeBook(order);
        }


    }

    public void cancelOrder(UUID orderId) {
        if (orderId == null) throw new IllegalArgumentException("Invalid order Id");

        Order order = orderMap.get(orderId);

        if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);

        if (order.isFilled()) throw new IllegalArgumentException("Order filled");

        Side side = order.getSide();
        TreeMap<BigDecimal, LinkedList<Order>> restingOrdersBook = side == Side.BUY ? bids : asks;

        LinkedList<Order> orderAtPrice = restingOrdersBook.get(order.getPrice());
        if (!orderAtPrice.isEmpty()) {
            orderAtPrice.remove(order);
            orderMap.remove(orderId);

            if (orderAtPrice.isEmpty()) {
                restingOrdersBook.remove(order.getPrice());
            }
        }

        order.cancel("Order initiator id");

    }

    private void matchOrder(Order incomingOrder, TreeMap<BigDecimal, LinkedList<Order>> oppositeOrdersTree, boolean isBidding) {

        while (!oppositeOrdersTree.isEmpty() && !incomingOrder.isFilled()) {
            BigDecimal bestOppositePrice = oppositeOrdersTree.firstKey();

            boolean canMatch = isBidding ? incomingOrder.getPrice().compareTo(bestOppositePrice) <= 0 :
                    incomingOrder.getPrice().compareTo(bestOppositePrice) >= 0;

            if (!canMatch) break;

            LinkedList<Order> restingOrders = oppositeOrdersTree.get(bestOppositePrice);

            if (restingOrders.isEmpty()) {
                oppositeOrdersTree.remove(bestOppositePrice);
                continue;
            }

            Order restingOrder = restingOrders.peekFirst();

            BigDecimal matchedQty = incomingOrder.getRemainingQuantity().min(restingOrder.getRemainingQuantity());

            incomingOrder.fill(matchedQty);
            restingOrder.fill(matchedQty);

            if (restingOrder.isFilled()) {
                restingOrders.remove(restingOrder);
                if (restingOrders.isEmpty()) {
                    oppositeOrdersTree.remove(bestOppositePrice);
                }
            }

            if (incomingOrder.isFilled()) {
                incomingOrder.setStatus(OrderStatus.FILLED);
            } else {
                incomingOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            incomingOrder.setTimestamp(LocalDateTime.now());
            incomingOrder.setUserId("user");

        }

    }

    private void addToOrderTreeBook(Order order) {
        TreeMap<BigDecimal, LinkedList<Order>> orderBook = order.getSide() == Side.BUY ? bids : asks;
        orderBook.computeIfAbsent(order.getPrice(), k -> new LinkedList<>())
                .addLast(order);
    }

    public BigDecimal getBestAskPrice(){
        return asks.isEmpty()? null: asks.firstKey();
    }

    public BigDecimal getBestBidPrice(){
        return bids.isEmpty()? null: bids.firstKey();
    }

}
