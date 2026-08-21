package com.dawood.nggeen.trade.bootstrap;

import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBookSnapshot;
import com.dawood.nggeen.trade.model.enums.InstrumentStatus;
import com.dawood.nggeen.trade.engine.OrderMatchingStrategy;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.infrastructure.persistence.InstrumentRepository;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.service.FileSnapShotStore;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderBookInitializer implements CommandLineRunner {
    private final InstrumentRepository instrumentRepository;
    private final OrderBookRegistry orderBookRegistry;
    private final Map<String, OrderMatchingStrategy> matchingStrategies;
    private final ChronicleQueueService chronicleQueueService;
    private final FileSnapShotStore fileSnapShotStore;

    @Getter
    @AllArgsConstructor
    public static class MarketContext {
        private OrderBook orderBook;
        private Instrument instrument;
    }

    @Override
    public void run(String... args) {
        try {
            List<OrderBookSnapshot> loadedSnapshots = new ArrayList<>();
            Map<String, MarketContext> marketContexts = initializedOrderBooks(loadedSnapshots);

            if (marketContexts.isEmpty()) {
                log.warn("No active markets to bootstrap.");
                return;
            }

            replayEvents(marketContexts, loadedSnapshots);
            registerOrderBook(marketContexts);

        } catch (Exception e) {
            log.error("Failed to initialize OrderBooks during startup", e);
            throw new IllegalStateException("OrderBook initialization failed", e);
        }

    }

    private Map<String, MarketContext> initializedOrderBooks(List<OrderBookSnapshot> loadedSnapshots) {
        Map<String, MarketContext> marketContextMap = new HashMap<>();

        List<Instrument> instruments = instrumentRepository.findByStatus(InstrumentStatus.TRADING);
        if (instruments.isEmpty()) {
            log.warn("No instruments found with status TRADING");
            return marketContextMap;
        }

        for (Instrument instrument : instruments) {
            String symbol = instrument.getSymbol();
            OrderBook orderBook = new OrderBook(matchingStrategies);
            orderBook.setInstrument(symbol);

            Optional<OrderBookSnapshot> snapshotOpt = fileSnapShotStore.loadLatest(symbol);
            if (snapshotOpt.isPresent()) {
                OrderBookSnapshot snapshot = snapshotOpt.get();
                orderBook.hydrateFromSnapshot(snapshot);
                loadedSnapshots.add(snapshot);
                log.info("Hydrated OrderBook [{}] from snapshot seq: {}", symbol, snapshot.getSequenceNo());
            } else {
                log.info("No existing snapshot found for [{}]. Starting clean book.", symbol);
            }
            marketContextMap.put(symbol, new MarketContext(orderBook, instrument));
        }
        return marketContextMap;
    }

    private void replayEvents(Map<String, MarketContext> marketContexts, List<OrderBookSnapshot> loadedSnapshots) {
        boolean missingAnySnapshot = loadedSnapshots.size() < marketContexts.size();
        long minStartIndex = missingAnySnapshot
                ? 0L
                : loadedSnapshots.stream()
                .mapToLong(OrderBookSnapshot::getChronicleQueueIndex)
                .min()
                .orElse(0L);

        log.info("Replaying delta events from Chronicle Queue starting at index: {}", minStartIndex);
        chronicleQueueService.replayFrom(minStartIndex, (eventType, event) -> {
            if (event == null || event.symbol() == null) {
                log.error("Event type or Order event is null");
                return;
            }

            MarketContext context = marketContexts.get(event.symbol());
            if (context == null) {
                log.error("No OrderBook found for {}", event.symbol());
                return;
            }
            OrderBook orderBook = context.getOrderBook();
            orderBook.rebuildOrderBookFromEventHistory(event);

        });
    }

    private void registerOrderBook(Map<String, MarketContext> marketContext) {
        for (MarketContext context : marketContext.values()) {
            OrderBook orderBook = context.getOrderBook();
            Instrument instrument = context.getInstrument();

            orderBookRegistry.registerOrderBook(orderBook, instrument);
            log.info("Registered OrderBook [{}] | Best Bid: {} | Sequence: {}",
                    orderBook.getInstrument(),
                    orderBook.getBestBidOrOffer(OrderSide.BUY),
                    orderBook.getSequenceGenerator().current());
        }
    }
}
