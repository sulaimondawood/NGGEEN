package com.dawood.nggeen.trade.config;

import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.model.enums.InstrumentStatus;
import com.dawood.nggeen.trade.engine.OrderMatchingStrategy;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.infrastructure.persistence.InstrumentRepository;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import com.dawood.nggeen.trade.service.SequenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderBookConfig implements CommandLineRunner {
    private final InstrumentRepository instrumentRepository;
    private final OrderBookRegistry orderBookRegistry;
    private final Map<String, OrderMatchingStrategy> matchingStrategies;
    private final ChronicleQueueService chronicleQueueService;


    @Override
    public void run(String... args) throws Exception {
        Map<String,OrderBook> orderBooks = new HashMap<>();

        List<Instrument> instruments = instrumentRepository.findByStatus(InstrumentStatus.TRADING);
        for (Instrument instrument : instruments) {
            OrderBook orderBook = new OrderBook(matchingStrategies);

            String symbol = instrument.getSymbol();
            orderBook.setInstrument(symbol);

            orderBooks.put(symbol, orderBook);
        }

        chronicleQueueService.replay((eventType, event) -> {
            if (event == null || event.symbol() == null) {
                log.error("Event type or Order event is null");
                return;
            }

            OrderBook orderBook = orderBooks.get(event.symbol());
            if (orderBook == null) {
                log.error("No OrderBook found for {}", event.symbol());
                return;
            }

            orderBook.rebuildOrderBookFromEventHistory(event);

        });

        for (OrderBook orderBook: orderBooks.values()){
            orderBookRegistry.registerOrderBook(orderBook);
            System.out.println(orderBook.getBestBidOrOffer(OrderSide.BUY));
            log.info("Registered OrderBook [{}] with starting sequence baseline: {}", orderBook.getInstrument(), orderBook.getSequenceGenerator().current());
        }
    }

}
