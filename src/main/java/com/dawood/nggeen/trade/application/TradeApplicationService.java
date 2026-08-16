package com.dawood.nggeen.trade.application;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.NggeenException;
import com.dawood.nggeen.trade.api.rest.dto.OrderResponse;
import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.model.enums.EventType;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeApplicationService {
    private final OrderBookRegistry orderBookRegistry;
    private final ChronicleQueueService chronicleQueueService;

    public void processIncomingOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Invalid Order Request");
        }

        OrderBook instrumentOrderBook = orderBookRegistry.getByInstrumentSymbol(orderRequest.getSymbol());
        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest, UuidCreator.getTimeOrderedEpoch());

        String symbol = incomingOrder.getSymbol();
        ExecutorService executor = orderBookRegistry.getExecutorFor(symbol);

        executor.submit(() -> processOrderSafely(instrumentOrderBook, incomingOrder, symbol));

    }

    public List<OrderResponse> getActiveOrders() {
        return orderBookRegistry.getAllOrderBooks().values().stream()
                .flatMap(orderBook -> orderBook.getActiveOrders().stream())
                .map(OrderMapper::toDTO)
                .toList();
    }

    private void processOrderSafely(OrderBook instrumentOrderBook, Order incomingOrder, String symbol) {
        try {
            long seq = instrumentOrderBook.getSequenceGenerator().next();

            DomainEvent acceptedEvent = incomingOrder.markAccepted(seq);
            chronicleQueueService.appendEvent(EventType.OrderAccepted, acceptedEvent);

            instrumentOrderBook.processOrder(incomingOrder);

        } catch (Exception e) {
            log.error("Failed to process order {} on symbol {}", incomingOrder.getId(), symbol, e);
            throw new NggeenException(
                    ErrorCode.ORDER_EXECUTION_FAILED,
                    "Order execution failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

}
