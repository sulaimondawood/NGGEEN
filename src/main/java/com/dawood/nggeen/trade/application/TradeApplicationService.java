package com.dawood.nggeen.trade.application;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import com.dawood.nggeen.trade.service.TradeExecutionDispatcher;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TradeApplicationService {
    private final OrderBookRegistry orderBookRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TradeExecutionDispatcher tradeExecutionDispatcher;

    public void processIncomingOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Invalid Order Request");
        }

        OrderBook instrumentOrderBook = orderBookRegistry.getByInstrumentSymbol(orderRequest.getSymbol());
        long sequenceNo = instrumentOrderBook.getSequenceGenerator().next();

        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest);
        incomingOrder.setId(UuidCreator.getTimeOrderedEpoch());
        incomingOrder.setSequenceNo(sequenceNo);
        incomingOrder.setStatus(OrderStatus.NEW);

        OrderAccepted acceptedOrder = OrderAccepted.builder()
                .orderId(incomingOrder.getId())
                .sequenceNo(incomingOrder.getSequenceNo())
                .symbol(incomingOrder.getSymbol())
                .timestamp(Instant.now())
                .orderType(incomingOrder.getOrderType())
                .orderSide(incomingOrder.getOrderSide())
                .price(incomingOrder.getPrice())
                .stopPrice(incomingOrder.getStopPrice())
                .quantity(incomingOrder.getQuantity())
                .build();

        applicationEventPublisher.publishEvent(acceptedOrder);

        tradeExecutionDispatcher.dispatch(instrumentOrderBook, incomingOrder);

    }

}
