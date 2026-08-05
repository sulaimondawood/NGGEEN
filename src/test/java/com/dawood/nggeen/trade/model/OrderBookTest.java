package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderType;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

@ExtendWith(MockitoExtension.class)
class OrderBookTest {
    private OrderBook orderBook;

    @Mock
    private OrderMatchingStrategy matchingStrategy;

    @BeforeEach
    void setup() {
        Map<String, OrderMatchingStrategy> matchingStrategies = Map.of("LIMIT", matchingStrategy, "MARKET", matchingStrategy);
        orderBook = new OrderBook(matchingStrategies);
    }

    @Nested
    class ProcessOrderTests {

        @Test
        void processOrderSuccessfullyForLimitOrder() {
            Order order = new Order();
            order.setOrderType(OrderType.LIMIT);

            orderBook.processOrder(order);

            Mockito.verify(matchingStrategy, Mockito.times(1)).match(order, orderBook);

        }

        @Test
        void processOrderSuccessfullyForMarketOrder() {
            Order order = new Order();
            order.setOrderType(OrderType.MARKET);

            orderBook.processOrder(order);

            Mockito.verify(matchingStrategy, Mockito.times(1)).match(order, orderBook);

        }

        @Test
        void shouldThrowExceptionWhenOrderIsNull() {
            IllegalArgumentException exception = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> orderBook.processOrder(null));

            Assertions.assertEquals("Invalid order", exception.getMessage());
            Mockito.verifyNoInteractions(matchingStrategy);
        }

        @Test
        void shouldThrowExceptionWhenMatchingStrategyIsNull() {
            Order order = new Order();
            order.setOrderType(OrderType.LIMIT);

            orderBook = new OrderBook(Map.of());

            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> orderBook.processOrder(order));

            Mockito.verifyNoInteractions(matchingStrategy);

        }

    }

    @Nested
    class GetBestBidOfferTests {
        @Test
        void getBestBidOrOfferShouldReturnBestOfferForBuyOrder() {
            OrderSide sell = OrderSide.SELL;
            Order ask1 = new Order();
            ask1.setOrderSide(sell);
            ask1.setPrice(new BigDecimal("1000"));

            Order ask2 = new Order();
            ask2.setOrderSide(sell);
            ask2.setPrice(new BigDecimal("1300"));

            Order ask3 = new Order();
            ask3.setOrderSide(sell);
            ask3.setPrice(new BigDecimal("1100"));

            orderBook.addOrderToBook(ask1);
            orderBook.addOrderToBook(ask2);
            orderBook.addOrderToBook(ask3);

            BigDecimal bestOffer = orderBook.getBestBidOrOffer(OrderSide.BUY);

            Assertions.assertEquals(new BigDecimal("1000"), bestOffer);

        }

        @Test
        void getBestBidOrOfferShouldReturnBestOfferForSellOrder() {

            Order buy1 = new Order();
            buy1.setPrice(new BigDecimal("2000"));
            buy1.setOrderSide(OrderSide.BUY);

            Order buy2 = new Order();
            buy2.setPrice(new BigDecimal("4000"));
            buy2.setOrderSide(OrderSide.BUY);

            Order buy3 = new Order();
            buy3.setPrice(new BigDecimal("1400"));
            buy3.setOrderSide(OrderSide.BUY);

            Order buy4 = new Order();
            buy4.setPrice(new BigDecimal("1800"));
            buy4.setOrderSide(OrderSide.BUY);

            orderBook.addOrderToBook(buy1);
            orderBook.addOrderToBook(buy2);
            orderBook.addOrderToBook(buy3);
            orderBook.addOrderToBook(buy4);

            BigDecimal bestBid = orderBook.getBestBidOrOffer(OrderSide.SELL);

            Assertions.assertEquals(new BigDecimal("4000"), bestBid);

        }

        @Test
        void getBestBidOrOfferShouldReturnNullWhenThereIsNoOrder(){
            Assertions.assertNull(orderBook.getBestBidOrOffer(OrderSide.BUY));
        }

        @Test
        void getBestBidOrOfferShouldReturnNullWhenThereIsNoBid(){
            Assertions.assertNull(orderBook.getBestBidOrOffer(OrderSide.SELL));
        }

    }

    @Nested
    class OppositeOrderBookSideTests{

        @Test
        void oppositeOrderBookShouldReturnBidOrdersForSell(){
            TreeMap<BigDecimal, LinkedList<Order>> result =
                    orderBook.oppositeOrderBookSide(OrderSide.SELL);

            Assertions.assertSame(orderBook.getBids(),result);

        }

        @Test
        void oppositeOrderBookShouldReturnBidOrdersForBuy(){
            TreeMap<BigDecimal, LinkedList<Order>> result =
                    orderBook.oppositeOrderBookSide(OrderSide.BUY);

            Assertions.assertSame(orderBook.getAsks(),result);

        }

    }

    @Nested
    class AddOrderToBookTests{

        @Test
        void addOrderToBookShouldSuccessfullyAddBuyOrderToBids(){
            Order buy = new Order();
            buy.setPrice(new BigDecimal("1000"));
            buy.setOrderSide(OrderSide.BUY);

            orderBook.addOrderToBook(buy);

            Assertions.assertTrue(orderBook.getBids().containsKey(new BigDecimal("1000")));

            LinkedList<Order> ordersAtPrice = orderBook.getBids().get(new BigDecimal("1000"));
            Assertions.assertNotNull(ordersAtPrice);
            Assertions.assertEquals(new BigDecimal("1000"), orderBook.getBids().firstKey());
            Assertions.assertEquals(buy, orderBook.getBids().get(new BigDecimal("1000")).getFirst());

        }

        @Test
        void addOrderToBookShouldSuccessfullyAddBuyOrderToAsks(){
            Order ask = new Order();
            ask.setPrice(new BigDecimal("1000"));
            ask.setOrderSide(OrderSide.SELL);

            orderBook.addOrderToBook(ask);

            Assertions.assertTrue(orderBook.getAsks().containsKey(new BigDecimal("1000")));

            LinkedList<Order> ordersAtPrice = orderBook.getAsks().get(new BigDecimal("1000"));
            Assertions.assertNotNull(ordersAtPrice);
            Assertions.assertEquals(new BigDecimal("1000"), orderBook.getAsks().firstKey());
            Assertions.assertEquals(ask, orderBook.getAsks().get(new BigDecimal("1000")).getFirst());

        }

        @Test
        void addOrderToBookShouldSuccessfullyAddOrderToTheSamePriceLevelIfThereIsAnExistingMatchingPrice(){
            Order buy = new Order();
            buy.setPrice(new BigDecimal("1000"));
            buy.setOrderSide(OrderSide.BUY);

            Order buy2 = new Order();
            buy2.setPrice(new BigDecimal("1000"));
            buy2.setOrderSide(OrderSide.BUY);

            Order buy3 = new Order();
            buy3.setPrice(new BigDecimal("1400"));
            buy3.setOrderSide(OrderSide.BUY);

            orderBook.addOrderToBook(buy);
            orderBook.addOrderToBook(buy2);
            orderBook.addOrderToBook(buy3);

            Assertions.assertEquals(2, orderBook.getBids().size());

            LinkedList<Order> ordersAt1000 = orderBook.getBids().get(new BigDecimal("1000"));
            Assertions.assertNotNull(ordersAt1000);
            Assertions.assertEquals(2, ordersAt1000.size());
            Assertions.assertEquals(buy, ordersAt1000.getFirst());
            Assertions.assertEquals(buy2, ordersAt1000.getLast());

        }
    }
}