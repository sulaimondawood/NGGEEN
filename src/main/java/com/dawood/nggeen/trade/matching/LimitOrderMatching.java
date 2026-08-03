package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.TreeMap;

@Component(value = "limit")
public class LimitOrderMatching implements OrderMatchingStrategy {
    @Override
    public void match(Order incomingOrder, OrderBook orderBook) {
        OrderSide orderSide = incomingOrder.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> oppositeOrders = orderBook.oppositeOrderBookSide(orderSide);

        if (oppositeOrders.isEmpty()) return;

        BigDecimal bestOffer = orderBook.getBestBidOrOffer(orderSide);
        LinkedList<Order> restingOrders = oppositeOrders.get(bestOffer);

        while(!restingOrders.isEmpty() && !incomingOrder.isFilled()){
            Order firstRestingOrder = restingOrders.getFirst();
            boolean matchablePrice = incomingOrder.matchablePrice(incomingOrder,firstRestingOrder);
            if(!matchablePrice) break;

            BigDecimal firstRestingOrderRemainingQty = firstRestingOrder.getRemainingQuantity();
            BigDecimal incomingOrderRemainingQty = incomingOrder.getRemainingQuantity();

            BigDecimal Qty = firstRestingOrderRemainingQty.compareTo(incomingOrderRemainingQty) <= 0? firstRestingOrderRemainingQty: incomingOrderRemainingQty;
        }

    }

}

