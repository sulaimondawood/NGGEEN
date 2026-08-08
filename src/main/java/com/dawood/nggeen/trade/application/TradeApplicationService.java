package com.dawood.nggeen.trade.application;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import com.dawood.nggeen.trade.service.TradeExecutionDispatcher;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeApplicationService {
    private final OrderBookRegistry orderBookRegistry;
    private final TradeExecutionDispatcher tradeExecutionDispatcher;

    public void processIncomingOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Invalid Order Request");
        }

        OrderBook instrumentOrderBook = orderBookRegistry.getByInstrumentSymbol(orderRequest.getSymbol());

        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest);
        incomingOrder.setId(UuidCreator.getTimeOrderedEpoch());
        incomingOrder.setStatus(OrderStatus.NEW);

        tradeExecutionDispatcher.dispatch(instrumentOrderBook, incomingOrder);

    }

}
