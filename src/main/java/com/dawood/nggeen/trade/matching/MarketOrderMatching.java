package com.dawood.nggeen.trade.matching;

import com.dawood.nggeen.trade.enums.CancelReason;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.UUID;

@Component(value = "MARKET")
public class MarketOrderMatching implements OrderMatchingStrategy {
    @Override
    public void match(Order incomingOrder, OrderBook orderBook) {
        OrderSide orderSide = incomingOrder.getOrderSide();
        TreeMap<BigDecimal, LinkedList<Order>> oppositeOrderSide = orderBook.oppositeOrderBookSide(orderSide);

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
            BigDecimal matchedQty = restingOrder.getRemainingQuantity().min(incomingOrder.getRemainingQuantity());
            restingOrder.fillQuantity(matchedQty);
            incomingOrder.fillQuantity(matchedQty);

            long tradeSeq = orderBook.getSequenceGenerator().next();
            UUID tradeId = UuidCreator.getTimeOrderedEpoch();
            UUID buyOrderId = (orderSide == OrderSide.BUY) ? incomingOrder.getId() : restingOrder.getId();
            UUID sellOrderId = (orderSide == OrderSide.SELL) ? incomingOrder.getId() : restingOrder.getId();

            DomainEvent event = new TradeExecuted(
                    tradeSeq,
                    tradeId,
                    buyOrderId,
                    sellOrderId,
                    incomingOrder.getSymbol(),
                    bestOffer,
                    matchedQty,
                    orderSide
            );
            incomingOrder.registerEvent(event);

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
            long cancelSeq = orderBook.getSequenceGenerator().next();
             handleResidualCancellation(
                    incomingOrder,
                    cancelSeq,
                    incomingOrder.getRemainingQuantity(),
                    CancelReason.NO_LIQUIDITY);
        }

    }

    private void handleResidualCancellation(Order incomingOrder, long seq, BigDecimal quantityCancelled, CancelReason reason) {
        OrderStatus finalStatus = incomingOrder.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0
                ? OrderStatus.PARTIALLY_FILLED
                : OrderStatus.CANCELED;
        incomingOrder.markCancelled(seq, quantityCancelled, reason, finalStatus);
//         TODO: Emit event to release unexecuted reserved wallet funds
    }
}
