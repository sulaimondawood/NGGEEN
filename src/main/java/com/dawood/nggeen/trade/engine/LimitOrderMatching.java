package com.dawood.nggeen.trade.engine;

import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.model.enums.EventType;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.UUID;

@Component(value = "LIMIT")
@RequiredArgsConstructor
@Slf4j
public class LimitOrderMatching implements OrderMatchingStrategy {
    private final ChronicleQueueService chronicleQueueService;

    @Override
    public void match(Order incomingOrder, OrderBook orderBook) {
        TreeMap<BigDecimal, LinkedList<Order>> oppositeOrders = orderBook.oppositeOrderBookSide(incomingOrder.getOrderSide());

        while (!oppositeOrders.isEmpty() && !incomingOrder.isFilled()) {
            BigDecimal bestOffer = orderBook.getBestBidOrOffer(incomingOrder.getOrderSide());
            if (bestOffer == null) break;

            LinkedList<Order> restingOrders = oppositeOrders.get(bestOffer);
            if (restingOrders == null || restingOrders.isEmpty()) {
                oppositeOrders.remove(bestOffer);
                continue;
            }

            Order firstRestingOrder = restingOrders.getFirst();

            boolean matchablePrice = incomingOrder.isMatchablePrice(firstRestingOrder);
            if (!matchablePrice) break;

            BigDecimal firstRestingOrderRemainingQty = firstRestingOrder.getRemainingQuantity();
            BigDecimal incomingOrderRemainingQty = incomingOrder.getRemainingQuantity();

            BigDecimal matchedQty = firstRestingOrderRemainingQty.compareTo(incomingOrderRemainingQty) <= 0 ?
                    firstRestingOrderRemainingQty : incomingOrderRemainingQty;

            firstRestingOrder.fillQuantity(matchedQty);
            incomingOrder.fillQuantity(matchedQty);

            OrderSide orderSide = incomingOrder.getOrderSide();
            long tradeSeq = orderBook.getSequenceGenerator().next();

            log.info(String.valueOf(tradeSeq));
            System.out.println(tradeSeq);

            UUID tradeId = UuidCreator.getTimeOrderedEpoch();
            UUID buyOrderId = (orderSide == OrderSide.BUY) ? incomingOrder.getId() : firstRestingOrder.getId();
            UUID sellOrderId = (orderSide == OrderSide.SELL) ? incomingOrder.getId() : firstRestingOrder.getId();

            DomainEvent tradedEvent = new TradeExecuted(
                    tradeSeq,
                    tradeId,
                    buyOrderId,
                    sellOrderId,
                    incomingOrder.getSymbol(),
                    bestOffer,
                    matchedQty,
                    orderSide
            );
            chronicleQueueService.appendEvent(EventType.TradeExecuted, tradedEvent);

            if (firstRestingOrder.isFilled()) {
                System.out.println("Filled");
                restingOrders.removeFirst();
                if (firstRestingOrder.getId() != null) {
                    orderBook.getOrderMap().remove(firstRestingOrder.getId());
                }
            }
            if (restingOrders.isEmpty()) {
                oppositeOrders.remove(bestOffer);
            }

        }

        if (incomingOrder.isFilled() && incomingOrder.getId() != null) {
            orderBook.getOrderMap().remove(incomingOrder.getId());
        }

        if (!incomingOrder.isFilled()) {
            orderBook.addOrderToBook(incomingOrder);
        }

    }

}

