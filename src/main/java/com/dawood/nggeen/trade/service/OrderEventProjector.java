package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.infrastructure.persistence.OrderRepository;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.enums.EventType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProjector {
    private static final int BATCH_SIZE = 100;

    private final ChronicleQueueService queueService;
    private final OrderRepository orderRepository;
    private ExcerptTailer namedTailer;

    @PostConstruct
    public void init() {
        namedTailer = queueService.createNamedTailer("order-projector");
    }

    @Scheduled(fixedDelay = 100)
    public void flush() {
        List<Order> batch = new ArrayList<>();
        long startIndex = namedTailer.index();

        while (batch.size() < BATCH_SIZE) {
            try (DocumentContext dc = namedTailer.readingDocument();) {
                if (!dc.isPresent()) {
                    break;
                }

                String eventType = dc.wire().read("eventType").text();
                if (Objects.equals(eventType, EventType.OrderAccepted.name())) {
                    OrderAccepted event = dc.wire().read("event").typedMarshallable();
                    if (event != null) {
                        batch.add(OrderMapper.fromEvent(event));
                    }
                } else {
                    dc.wire().read("event").typedMarshallable();

                }

            }

        }
        if (!batch.isEmpty()) {
            try {
                orderRepository.saveAll(batch);
            } catch (Exception e) {
                log.error("Failed to project {} orders", batch.size(), e);
                namedTailer.moveToIndex(startIndex);
            }
        }
    }

}
