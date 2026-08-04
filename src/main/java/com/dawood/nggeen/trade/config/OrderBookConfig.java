package com.dawood.nggeen.trade.config;

import com.dawood.nggeen.trade.enums.InstrumentStatus;
import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.repository.InstrumentRepository;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderBookConfig implements CommandLineRunner {
    private final InstrumentRepository instrumentRepository;
    private final OrderBookRegistry orderBookRegistry;
    private final OrderMatchingStrategy matchingStrategy;

    @Override
    public void run(String... args) throws Exception {
        List<Instrument> instruments = instrumentRepository.findByStatus(InstrumentStatus.TRADING);

        for (Instrument instrument: instruments){
            OrderBook orderBook = new OrderBook(matchingStrategy);
            orderBookRegistry.addOrderBook(instrument, orderBook);
        }

    }
}
