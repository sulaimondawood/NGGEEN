package com.dawood.nggeen.trade.application;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.infrastructure.persistence.OrderRepository;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.model.enums.EventType;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeApplicationService {
    private final OrderBookRegistry orderBookRegistry;
    private final OrderRepository orderRepository;
    private final ChronicleQueueService chronicleQueueService;

    public void processIncomingOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Invalid Order Request");
        }

        OrderBook instrumentOrderBook = orderBookRegistry.getByInstrumentSymbol(orderRequest.getSymbol());
        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest);
        incomingOrder.setId(UuidCreator.getTimeOrderedEpoch());

        String symbol = incomingOrder.getSymbol();
        ExecutorService executor = orderBookRegistry.getExecutorFor(symbol);

        executor.submit(() -> {
            try {
                long seq = instrumentOrderBook.getSequenceGenerator().next();
                incomingOrder.setSequenceNo(seq);

                DomainEvent acceptedEvent = incomingOrder.markAccepted(seq);
                chronicleQueueService.appendEvent(EventType.OrderAcceptedEvent, acceptedEvent);

                instrumentOrderBook.trackDirtyOrders(incomingOrder);

                instrumentOrderBook.processOrder(incomingOrder);

                List<Order> dirtyOrders = instrumentOrderBook.getAndClearDirtyOrders();
                if (!dirtyOrders.isEmpty()) {
                    orderRepository.saveAll(dirtyOrders);
                }

            } catch (Exception e) {
                log.error("Failed to process order {} on symbol {}", incomingOrder.getId(), symbol, e);
            }
        });


    }

}
