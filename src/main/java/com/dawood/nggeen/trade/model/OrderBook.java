package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.engine.OrderMatchingStrategy;
import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.event.OrderCancelled;
import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import com.dawood.nggeen.trade.model.enums.OrderType;
import com.dawood.nggeen.trade.service.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@Slf4j
public class OrderBook {
    private Map<String, OrderMatchingStrategy> matchingStrategies;
    private SequenceGenerator sequenceGenerator = new SequenceGenerator();

    private String instrument;
    private TreeMap<BigDecimal, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();
    private Map<UUID, Order> orderMap = new HashMap<>();

    @Getter(AccessLevel.NONE)
    private Set<Order> dirtyOrders = new LinkedHashSet<>();

    public OrderBook(Map<String, OrderMatchingStrategy> strategy) {
        this.matchingStrategies = strategy;
    }

    public void processOrder(Order incomingOrder) {
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
        TreeMap<BigDecimal, LinkedList<Order>> orderBookSide = getOrderBookSide(orderSide);
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

    public TreeMap<BigDecimal, LinkedList<Order>> getOrderBookSide(OrderSide orderSide) {
        return orderSide == OrderSide.BUY ? bids : asks;
    }

    public void trackDirtyOrders(Order order) {
        if (order != null) {
            this.dirtyOrders.add(order);
        }
    }

    public List<Order> getAndClearDirtyOrders() {
        List<Order> dirtyOrdersCopy = new ArrayList<>(dirtyOrders);
        dirtyOrders.clear();
        return dirtyOrdersCopy;
    }

    public void rebuildOrderBookFromEventHistory(DomainEvent event) {
        if(event == null){
            return;
        }
        sequenceGenerator.updateIfGreater(event.sequenceNo());
        switch (event) {
            case OrderAccepted accepted -> replayOrderAccepted(accepted);

            case TradeExecuted traded -> replayTradeExecuted(traded);

            case OrderCancelled cancelled -> replayOrderCancelled(cancelled);

          default -> log.debug("Unhandled event type during replay: {}", event.getClass().getSimpleName());
        }

    }

    private BigDecimal getBestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    private BigDecimal getBestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    private void replayOrderAccepted(OrderAccepted event) {
        Order order = Order.buildOrderFromEvent(event);
        if (order.getOrderType() == OrderType.LIMIT) {
            addOrderToBook(order);
        }
    }

    private void replayTradeExecuted(TradeExecuted event) {
        Order buyOrder = orderMap.get(event.getBuyOrderId());
        if (buyOrder != null) {
            buyOrder.fillQuantity(event.getExecutedQuantity());

            if (buyOrder.isFilled()) {
                removeOrderFromBook(buyOrder);
            }
        }

        Order sellOrder = orderMap.get(event.getSellOrderId());
        if (sellOrder != null) {
            sellOrder.fillQuantity(event.getExecutedQuantity());

            if (sellOrder.isFilled()) {
                removeOrderFromBook(sellOrder);
            }
        }
    }

    private void replayOrderCancelled(OrderCancelled event) {
        Order order = orderMap.get(event.getOrderId());
        if (order == null) {
            return;
        }
        removeOrderFromBook(order);
    }

    private void removeOrderFromBook(Order order) {
        TreeMap<BigDecimal, LinkedList<Order>> orderBookSide =
                getOrderBookSide(order.getOrderSide());

        LinkedList<Order> ordersAtPrice = orderBookSide.get(order.getPrice());

        if (ordersAtPrice == null) {
            return;
        }

        ordersAtPrice.remove(order);

        if (ordersAtPrice.isEmpty()) {
            orderBookSide.remove(order.getPrice());
        }

        orderMap.remove(order.getId());
    }

}
