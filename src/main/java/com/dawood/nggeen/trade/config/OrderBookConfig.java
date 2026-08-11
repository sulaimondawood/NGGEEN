package com.dawood.nggeen.trade.config;

import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.model.enums.InstrumentStatus;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.infrastructure.persistence.InstrumentRepository;
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
        Map<String, Long> maxSequences = new HashMap<>();
        chronicleQueueService.replay((eventType, event) -> {
            if (event != null && event.symbol() != null) {
                maxSequences.merge(event.symbol(), event.sequenceNo(), Math::max);
            }
        });

        List<Instrument> instruments = instrumentRepository.findByStatus(InstrumentStatus.TRADING);
        for (Instrument instrument : instruments) {
            OrderBook orderBook = new OrderBook(matchingStrategies);

            String symbol = instrument.getSymbol();
            orderBook.setInstrument(symbol);

            long maxSeq = maxSequences.getOrDefault(symbol, 0L);
            orderBook.setSequenceGenerator(new SequenceGenerator(maxSeq));

            orderBookRegistry.registerOrderBook(orderBook);
            log.info("Registered OrderBook [{}] with starting sequence baseline: {}", symbol, maxSeq);
        }
    }

}
