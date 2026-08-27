package com.dawood.nggeen.trade.engine;

import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.model.enums.CancelReason;
import com.dawood.nggeen.trade.model.enums.EventType;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.UUID;

@Component(value = "MARKET")
@RequiredArgsConstructor
@Slf4j
public class MarketOrderMatching implements OrderMatchingStrategy {
    private final ChronicleQueueService chronicleQueueService;

    @Override
    public void match(Order incomingOrder, OrderBook orderBook) {
        OrderSide orderSide = incomingOrder.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> oppositeOrderSide = orderBook.oppositeOrderBookSide(orderSide);

        BigDecimal cumulativeQuoteSpent = BigDecimal.ZERO;

        while (!oppositeOrderSide.isEmpty() && !incomingOrder.isFilled()) {
            BigDecimal bestOffer = orderBook.getBestBidOrOffer(orderSide);
            if (bestOffer == null) {
                break;
            }

            LinkedList<Order> restingOrdersAtPriceLevel = oppositeOrderSide.get(bestOffer);
            if (restingOrdersAtPriceLevel.isEmpty()) {
                oppositeOrderSide.remove(bestOffer);
                continue;
            }

            Order restingOrder = restingOrdersAtPriceLevel.getFirst();
            if (incomingOrder.isSelfTrade(restingOrder)) {
                log.debug("Self trade cancel triggered for account {}. Halting taker execution.", incomingOrder.getAccountId());
                break;
            }

            BigDecimal matchedQty = restingOrder.getRemainingQuantity().min(incomingOrder.getRemainingQuantity());
            BigDecimal tradeQuoteAmount = bestOffer.multiply(matchedQty);
            cumulativeQuoteSpent = cumulativeQuoteSpent.add(tradeQuoteAmount);

            restingOrder.fillQuantity(matchedQty);
            incomingOrder.fillQuantity(matchedQty);

            long tradeSeq = orderBook.getSequenceGenerator().next();
            UUID tradeId = UuidCreator.getTimeOrderedEpoch();

            UUID buyOrderId = (orderSide == OrderSide.BUY) ? incomingOrder.getId() : restingOrder.getId();
            UUID sellOrderId = (orderSide == OrderSide.SELL) ? incomingOrder.getId() : restingOrder.getId();
            UUID buyAccountId = (orderSide == OrderSide.BUY) ? incomingOrder.getAccountId() : restingOrder.getAccountId();
            UUID sellAccountId = (orderSide == OrderSide.SELL) ? incomingOrder.getAccountId() : restingOrder.getAccountId();

            DomainEvent tradedEvent = new TradeExecuted(
                    tradeSeq,
                    tradeId,
                    buyOrderId,
                    sellOrderId,
                    buyAccountId,
                    sellAccountId,
                    incomingOrder.getSymbol(),
                    incomingOrder.getQuoteAsset(),
                    incomingOrder.getBaseAsset(),
                    bestOffer,
                    matchedQty,
                    orderSide
            );

            chronicleQueueService.appendEvent(EventType.TradeExecuted, tradedEvent);

            if (restingOrder.isFilled()) {
                restingOrdersAtPriceLevel.removeFirst();
                if (restingOrder.getId() != null) {
                    orderBook.getOrderMap().remove(restingOrder.getId());
                }
            }

            if (restingOrdersAtPriceLevel.isEmpty()) {
                oppositeOrderSide.remove(bestOffer);
            }

        }

        if (!incomingOrder.isFilled()) {
            handleResidualCancellation(incomingOrder, orderBook, cumulativeQuoteSpent);
        }

    }

    private void handleResidualCancellation(Order incomingOrder, OrderBook orderBook, BigDecimal cumulativeQuoteSpent) {
        long cancelSeq = orderBook.getSequenceGenerator().next();
        BigDecimal unfilledQty = incomingOrder.getRemainingQuantity();
        UUID accountId = incomingOrder.getAccountId();
        OrderStatus finalStatus = incomingOrder.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0
                ? OrderStatus.PARTIALLY_FILLED
                : OrderStatus.CANCELED;

        String assetToLock;
        BigDecimal amountToRelease;

        if (incomingOrder.getOrderSide() == OrderSide.BUY) {
            assetToLock = incomingOrder.getQuoteAsset();
            amountToRelease = incomingOrder.getLockedAmount().subtract(cumulativeQuoteSpent);
        } else {
            assetToLock = incomingOrder.getBaseAsset();
            amountToRelease = unfilledQty;
        }

        DomainEvent canceledEvent = incomingOrder.markCancelled(
                cancelSeq,
                unfilledQty,
                CancelReason.NO_LIQUIDITY,
                finalStatus,
                assetToLock,
                accountId,
                amountToRelease
        );
        chronicleQueueService.appendEvent(EventType.OrderCancelled, canceledEvent);
    }
}
