package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LimitOrderMatchingTest {
    private final OrderMatchingStrategy limitOrderMatchingStrategy = new LimitOrderMatching();
    private final OrderBook orderBook = new OrderBook(Map.of());

    private Order createOrder(OrderSide side, String qty, String price) {
        Order order = new Order();
        order.setOrderSide(side);
        order.setQuantity(new BigDecimal(qty));
        order.setRemainingQuantity(new BigDecimal(qty));
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setPrice(price != null ? new BigDecimal(price) : null);
        order.setStatus(OrderStatus.NEW);
        return order;
    }

    @Test
    void shouldAddOrderToBookIfNoRestingOppositeOrders(){
        Order buyOrder = createOrder(OrderSide.BUY,"5.0", "1000.0");

        limitOrderMatchingStrategy.match(buyOrder, orderBook);

        assertEquals(new BigDecimal("5.0"), buyOrder.getRemainingQuantity());
        assertFalse(buyOrder.isFilled());
        assertTrue(orderBook.getBids().containsKey(new BigDecimal("1000.0")));
        assertEquals(1, orderBook.getBids().size());
    }

    @Test
    void shouldMoveToNextPriceLevelIfCurrentPriceLevelHasNoRestingOrdersButHasAssociatedPriceValue(){
        Order buyOrder = createOrder(OrderSide.BUY,"5.0", "1000.0");
        Order restingAsk1 = createOrder(OrderSide.SELL, "2.0", "1000.0");
        Order restingAsk2 = createOrder(OrderSide.SELL, "2.0", "1100.0");
        Order restingAsk3 = createOrder(OrderSide.SELL, "3.0", "990.0");
        orderBook.getAsks().put(new BigDecimal("980.0"), new LinkedList<>());

        orderBook.addOrderToBook(restingAsk1);
        orderBook.addOrderToBook(restingAsk2);
        orderBook.addOrderToBook(restingAsk3);

        limitOrderMatchingStrategy.match(buyOrder, orderBook);

        assertFalse(orderBook.getAsks().containsKey(new BigDecimal("980.0")));
        assertTrue(restingAsk3.isFilled());
        assertEquals(new BigDecimal("0.0"), restingAsk3.getRemainingQuantity());

        assertFalse(restingAsk2.isFilled());
        assertTrue(restingAsk1.isFilled());
    }

//    void shouldMatchOrderWithRestingO

}