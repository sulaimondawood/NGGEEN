package com.dawood.nggeen.trade.config;

import com.dawood.nggeen.trade.model.enums.InstrumentStatus;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.infrastructure.persistence.InstrumentRepository;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderBookConfig implements CommandLineRunner {
    private final InstrumentRepository instrumentRepository;
    private final OrderBookRegistry orderBookRegistry;
    private final Map<String, OrderMatchingStrategy> matchingStrategies;

    @Override
    public void run(String... args) throws Exception {
        List<Instrument> instruments = instrumentRepository.findByStatus(InstrumentStatus.TRADING);

        for (Instrument instrument : instruments) {
            OrderBook orderBook = new OrderBook(matchingStrategies);
            orderBook.setInstrument(instrument.getSymbol());
            orderBookRegistry.registerOrderBook(orderBook);
        }

    }
}
