package com.dawood.nggeen.trade.matching;

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
        TreeMap<BigDecimal, LinkedList<Order>> oppositeOrders = orderBook.oppositeOrderBookSide(incomingOrder.getOrderSide());

        while (!oppositeOrders.isEmpty() && !incomingOrder.isFilled()) {
            BigDecimal bestOffer = orderBook.getBestBidOrOffer(incomingOrder.getOrderSide());
            if (bestOffer == null) break;

            LinkedList<Order> restingOrders = oppositeOrders.get(bestOffer);
            if(restingOrders ==null || restingOrders.isEmpty()){
                oppositeOrders.remove(bestOffer);
                continue;
            }

            Order firstRestingOrder = restingOrders.getFirst();

            boolean matchablePrice = incomingOrder.isMatchablePrice(firstRestingOrder);
            if (!matchablePrice) break;

            BigDecimal firstRestingOrderRemainingQty = firstRestingOrder.getRemainingQuantity();
            BigDecimal incomingOrderRemainingQty = incomingOrder.getRemainingQuantity();

            BigDecimal matchedQty = firstRestingOrderRemainingQty.compareTo(incomingOrderRemainingQty) <= 0 ? firstRestingOrderRemainingQty : incomingOrderRemainingQty;

            firstRestingOrder.fillQuantity(matchedQty);
            incomingOrder.fillQuantity(matchedQty);

            if (firstRestingOrder.isFilled()) {
                restingOrders.removeFirst();
            }
            if(restingOrders.isEmpty()){
                oppositeOrders.remove(bestOffer);
            }

        }

        if(!incomingOrder.isFilled()){
            orderBook.addOrderToBook(incomingOrder);
        }

    }

}

