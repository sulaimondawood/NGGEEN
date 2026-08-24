package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.account.application.LedgerSettlementService;
import com.dawood.nggeen.trade.event.OrderCancelled;
import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.infrastructure.persistence.TradeRepository;
import com.dawood.nggeen.trade.mapper.TradeMapper;
import com.dawood.nggeen.trade.model.Trade;
import com.dawood.nggeen.trade.model.enums.EventType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventProjector {
    private static final int BATCH_SIZE = 500;

    private final TradeRepository tradeRepository;
    private final ChronicleQueueService queueService;
    private final LedgerSettlementService ledgerSettlementService;
    private final TransactionTemplate transactionTemplate;

    private ExcerptTailer namedTailer;

    @PostConstruct
    public void init() {
        namedTailer = queueService.createNamedTailer("trade-projector");
    }

    @Scheduled(fixedDelay = 200)
    public void flush() {
        long startIdx = namedTailer.index();
        List<TradeExecuted> tradeExecutedEvents = new ArrayList<>(BATCH_SIZE);
        List<OrderCancelled> orderEventCancelledEvents = new ArrayList<>();
        int reads = 0;

        while (reads < BATCH_SIZE) {
            try (DocumentContext dc = namedTailer.readingDocument()) {
                if (!dc.isPresent()) {
                    break;
                }
                String eventType = dc.wire().read("eventType").text();
                if (Objects.equals(eventType, EventType.TradeExecuted.name())) {
                    TradeExecuted event = dc.wire().read("event").typedMarshallable();
                    if (event != null) {
                        tradeExecutedEvents.add(event);
                    }
                } else if (Objects.equals(EventType.OrderCancelled.name(), eventType)) {
                    OrderCancelled eventCancelled = dc.wire().read("event").typedMarshallable();
                    if (eventCancelled != null) {
                        orderEventCancelledEvents.add(eventCancelled);
                    }
                }
                reads++;
            }
        }
        if (tradeExecutedEvents.isEmpty() && orderEventCancelledEvents.isEmpty()) {
            return;
        }

        try {
            transactionTemplate.executeWithoutResult((status) -> {
                processBatchInTransaction(tradeExecutedEvents, orderEventCancelledEvents);

            });
        } catch (Exception e) {
            log.error("Failed to project {} trades", tradeExecutedEvents.size(), e);
            namedTailer.moveToIndex(startIdx);
        }

    }

    private void processBatchInTransaction(List<TradeExecuted> tradesExecuted, List<OrderCancelled> ordersCancelled) {
        List<Trade> trades = new ArrayList<>();

        for (TradeExecuted tradeExecuted : tradesExecuted) {
            trades.add(TradeMapper.fromEvent(tradeExecuted));
            ledgerSettlementService.processTradeExecution(tradeExecuted);
        }

        for (OrderCancelled orderCancelled : ordersCancelled) {
            ledgerSettlementService.processOrderCancellation(orderCancelled);
        }

        if (!trades.isEmpty()) {
            tradeRepository.saveAll(trades);
        }

        log.debug("Atomically settled and saved {} trades and {} cancellations.", tradesExecuted.size(), ordersCancelled.size());
    }
}
