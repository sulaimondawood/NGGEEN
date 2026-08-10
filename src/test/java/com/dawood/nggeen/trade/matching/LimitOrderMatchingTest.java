package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class ImmediateMatchingTests {
        @Test
        void shouldFullyMatchAndRemoveBothOrdersWhenQuantitiesAndPricesMatchExactly() {
            Order restingAsk = createOrder(OrderSide.SELL, "5.0", "100.0");
            orderBook.addOrderToBook(restingAsk);

            Order incomingBuy = createOrder(OrderSide.BUY, "5.0", "100.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertTrue(incomingBuy.isFilled());
            assertTrue(restingAsk.isFilled());
            assertTrue(orderBook.getAsks().isEmpty());
            assertTrue(orderBook.getBids().isEmpty());
        }

        @Test
        void shouldExecuteTradeWhenIncomingBuyPriceIsBetterThanRestingAskPrice() {
            Order restingAsk = createOrder(OrderSide.SELL, "5.0", "100.0");
            orderBook.addOrderToBook(restingAsk);

            // Incoming buy willing to pay up to 105.0
            Order incomingBuy = createOrder(OrderSide.BUY, "5.0", "105.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertTrue(incomingBuy.isFilled());
            assertTrue(restingAsk.isFilled());
            assertTrue(orderBook.getAsks().isEmpty());
        }

        @Test
        void shouldNotMatchWhenIncomingBuyPriceIsLowerThanBestAskPrice() {
            Order restingAsk = createOrder(OrderSide.SELL, "5.0", "100.0");
            orderBook.addOrderToBook(restingAsk);

            Order incomingBuy = createOrder(OrderSide.BUY, "5.0", "95.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertFalse(incomingBuy.isFilled());
            assertFalse(restingAsk.isFilled());

            assertTrue(orderBook.getAsks().containsKey(new BigDecimal("100.0")));
            assertTrue(orderBook.getBids().containsKey(new BigDecimal("95.0")));
        }

    }

    @Nested
    class PartialFillAndResidualHandlingTests {

        @Test
        void shouldPartiallyFillRestingOrderAndKeepItInBookWhenIncomingIsSmaller() {
            Order restingAsk = createOrder(OrderSide.SELL, "10.0", "100.0");
            orderBook.addOrderToBook(restingAsk);

            Order incomingBuy = createOrder(OrderSide.BUY, "4.0", "100.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertTrue(incomingBuy.isFilled());
            assertFalse(restingAsk.isFilled());

            assertEquals(0, restingAsk.getRemainingQuantity().compareTo(new BigDecimal("6.0")));
            assertTrue(orderBook.getAsks().containsKey(new BigDecimal("100.0")));
            assertTrue(orderBook.getBids().isEmpty());
        }

        @Test
        void shouldFullyFillRestingOrderAndRestRemainingIncomingQuantityInBook() {
            Order restingAsk = createOrder(OrderSide.SELL, "4.0", "100.0");
            orderBook.addOrderToBook(restingAsk);

            Order incomingBuy = createOrder(OrderSide.BUY, "10.0", "100.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertTrue(restingAsk.isFilled());
            assertFalse(incomingBuy.isFilled());

            assertEquals(0, incomingBuy.getRemainingQuantity().compareTo(new BigDecimal("6.0")));
            assertTrue(orderBook.getAsks().isEmpty());
            assertTrue(orderBook.getBids().containsKey(new BigDecimal("100.0")));
            assertEquals(1, orderBook.getBids().size());
        }

        @Test
        void shouldAddIncomingOrderToBookWhenOrderBookIsEmpty() {
            Order incomingBuy = createOrder(OrderSide.BUY, "5.0", "100.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertFalse(incomingBuy.isFilled());
            assertTrue(orderBook.getBids().containsKey(new BigDecimal("100.0")));
            assertEquals(1, orderBook.getBids().get(new BigDecimal("100.0")).size());
        }
    }

    @Nested
    class QueueAndMultiLevelExecutionTests {

        @Test
        void shouldMaintainFifoOrderWhenMatchingAtSamePriceLevel() {
            Order ask1 = createOrder(OrderSide.SELL, "3.0", "100.0");
            Order ask2 = createOrder(OrderSide.SELL, "5.0", "100.0");
            orderBook.addOrderToBook(ask1);
            orderBook.addOrderToBook(ask2);

            Order incomingBuy = createOrder(OrderSide.BUY, "3.0", "100.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertTrue(ask1.isFilled());
            assertFalse(ask2.isFilled());
            assertEquals(1, orderBook.getAsks().get(new BigDecimal("100.0")).size());
            assertEquals(ask2, orderBook.getAsks().get(new BigDecimal("100.0")).getFirst());
        }

        @Test
        void shouldCleanOrphanEmptyPriceBucketAndContinueMatching() {
            Order restingAsk = createOrder(OrderSide.SELL, "5.0", "105.0");
            orderBook.addOrderToBook(restingAsk);


            orderBook.getAsks().put(new BigDecimal("100.0"), new LinkedList<>());

            Order incomingBuy = createOrder(OrderSide.BUY, "5.0", "105.0");

            limitOrderMatchingStrategy.match(incomingBuy, orderBook);

            assertFalse(orderBook.getAsks().containsKey(new BigDecimal("100.0")));
            assertTrue(incomingBuy.isFilled());
            assertTrue(restingAsk.isFilled());
        }

        @Test
        void shouldMoveToNextPriceLevelIfCurrentPriceLevelHasNoRestingOrdersButHasAssociatedPriceValue() {
            Order buyOrder = createOrder(OrderSide.BUY, "5.0", "1000.0");
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

    }
}