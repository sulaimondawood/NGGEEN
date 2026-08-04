package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final OrderBookRegistry orderBookRegistry;

    public void processIncomingOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Invalid Order Request");
        }
        OrderBook instrumentOrderBook = orderBookRegistry.getByInstrumentSymbol(orderRequest.getSymbol());
        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest);
        instrumentOrderBook.processOrder(incomingOrder);

    }

}
