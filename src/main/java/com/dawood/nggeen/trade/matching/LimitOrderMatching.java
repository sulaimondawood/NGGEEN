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
    public void match(PlaceOrderRequest orderRequest, OrderBook orderBook) {
        OrderSide orderSide = orderRequest.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> oppositeOrders = orderBook.oppositeOrderBookSide(orderSide);

        if(oppositeOrders.isEmpty()) return;

        BigDecimal bestOffer = orderBook.getBestBidOrOffer(orderSide);
        LinkedList<Order> restingOrders = oppositeOrders.get(bestOffer);

        while (!restingOrders.isEmpty() && orderRequest)
    }

}
}
