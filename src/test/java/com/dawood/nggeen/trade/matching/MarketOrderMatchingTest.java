package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarketOrderMatchingTest {

    private MarketOrderMatching marketOrderMatching;
    private OrderBook orderBook;

    @BeforeEach
    void setup() {
        marketOrderMatching = new MarketOrderMatching();
        orderBook = new OrderBook(Map.of());
    }

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
    void shouldFullyMatchMarketBuyOrderAgainstSingleRestingSellOrderWithoutRemainQty() {
        Order restingSellOrder = createOrder(OrderSide.SELL, "5.0", "1000.0");
        orderBook.addOrderToBook(restingSellOrder);
        Order incomingMarketBuy = createOrder(OrderSide.BUY, "5.0", "1000.0");

        marketOrderMatching.match(incomingMarketBuy, orderBook);

        assertTrue(incomingMarketBuy.isFilled());
        assertEquals(new BigDecimal("5.0"), incomingMarketBuy.getFilledQuantity());
        assertEquals(new BigDecimal("0.0"), incomingMarketBuy.getRemainingQuantity());


        assertTrue(orderBook.getAsks().isEmpty());
        assertEquals(new BigDecimal("5.0"), restingSellOrder.getFilledQuantity());
        assertTrue(restingSellOrder.isFilled());

    }

    @Test
    void shouldFullyMatchMarketBuyOrderAgainstSingleRestingSellOrderWithRemainQty() {
        Order restingSellOrder = createOrder(OrderSide.SELL, "10.0", "1000.0");
        orderBook.addOrderToBook(restingSellOrder);
        Order incomingMarketBuy = createOrder(OrderSide.BUY, "5.0", "1000.0");

        marketOrderMatching.match(incomingMarketBuy, orderBook);

        assertTrue(incomingMarketBuy.isFilled());
        assertEquals(new BigDecimal("5.0"), incomingMarketBuy.getFilledQuantity());
        assertEquals(new BigDecimal("0.0"), incomingMarketBuy.getRemainingQuantity());


        assertFalse(orderBook.getAsks().isEmpty());
        assertEquals(new BigDecimal("5.0"), restingSellOrder.getFilledQuantity());
        assertEquals(new BigDecimal("5.0"), restingSellOrder.getRemainingQuantity());
        assertFalse(restingSellOrder.isFilled());

    }

    @Test
    void shouldSweepMultiplePriceLevelsForLargeMarketOrder() {
        Order incomingMarketBuy = createOrder(OrderSide.BUY, "5", "1150.0");

        Order ask1 = createOrder(OrderSide.SELL, "3", "1200.0");
        Order ask2 = createOrder(OrderSide.SELL, "1", "1100.0");
        Order ask3 = createOrder(OrderSide.SELL, "5", "1100.0");

        orderBook.addOrderToBook(ask1);
        orderBook.addOrderToBook(ask2);
        orderBook.addOrderToBook(ask3);


        marketOrderMatching.match(incomingMarketBuy,orderBook);

        assertTrue(incomingMarketBuy.isFilled());
        assertEquals(new BigDecimal("0"), incomingMarketBuy.getRemainingQuantity());
        assertEquals(new BigDecimal("5"), incomingMarketBuy.getFilledQuantity());

        assertFalse(ask1.isFilled());
        assertTrue(orderBook.getAsks().containsKey(new BigDecimal("1200.0")));
        assertEquals(new BigDecimal("3"), ask1.getRemainingQuantity());
        assertEquals(new BigDecimal("0"), ask1.getFilledQuantity());

        assertTrue(ask2.isFilled());
        assertEquals(new BigDecimal("0"), ask2.getRemainingQuantity());
        assertEquals(new BigDecimal("1"), ask2.getFilledQuantity());

        assertFalse(ask3.isFilled());
        assertTrue(orderBook.getAsks().containsKey(new BigDecimal("1100.0")));
        assertEquals(1, orderBook.getAsks().get(new BigDecimal("1100.0")).size());
        assertEquals(new BigDecimal("1"), ask3.getRemainingQuantity());
        assertEquals(new BigDecimal("4"), ask3.getFilledQuantity());

    }

    @Test
    void shouldMarkOrderAsPartiallyFilledWhenDepthIsInsufficient(){
        Order incomingBuyOrder = createOrder(OrderSide.BUY, "5.0", "1000");
        Order restingAsk1 = createOrder(OrderSide.SELL, "3.0", "1000");
        orderBook.addOrderToBook(restingAsk1);

        marketOrderMatching.match(incomingBuyOrder,orderBook);

        assertEquals(OrderStatus.PARTIALLY_FILLED, incomingBuyOrder.getStatus());
        assertEquals(new BigDecimal("3.0"), incomingBuyOrder.getFilledQuantity());
        assertEquals(new BigDecimal("2.0"), incomingBuyOrder.getRemainingQuantity());

        assertTrue(orderBook.getAsks().isEmpty());
    }

    @Test
    void shouldNotRunMatchingOrderEngineIfThereAreNoOrdersInBook(){
        Order incomingMarketBuy = createOrder(OrderSide.BUY, "5.0", "1000.0");

        marketOrderMatching.match(incomingMarketBuy, orderBook);

        assertEquals(new BigDecimal("5.0"), incomingMarketBuy.getRemainingQuantity());
        assertEquals(new BigDecimal("0"), incomingMarketBuy.getFilledQuantity());
        assertEquals(OrderStatus.CANCELED, incomingMarketBuy.getStatus());
    }

    @Test
    void shouldSkipAndRemoveEmptyPriceLevelQueueInBook() {
        Order restingAsk = createOrder(OrderSide.SELL, "5.0", "1200.0");
        orderBook.addOrderToBook(restingAsk);

        orderBook.getAsks().put(new BigDecimal("1100.0"), new LinkedList<>());

        Order incomingMarketBuy = createOrder(OrderSide.BUY, "5.0", null);

        marketOrderMatching.match(incomingMarketBuy, orderBook);

        assertTrue(incomingMarketBuy.isFilled());
        assertFalse(orderBook.getAsks().containsKey(new BigDecimal("1100.0"))); // Key removed
        assertTrue(orderBook.getAsks().isEmpty()); // Book clean
    }
}