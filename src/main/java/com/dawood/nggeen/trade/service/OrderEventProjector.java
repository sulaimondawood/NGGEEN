package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.event.OrderCancelled;
import com.dawood.nggeen.trade.event.TradeExecuted;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.infrastructure.persistence.OrderRepository;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.enums.EventType;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProjector {
    private static final int BATCH_SIZE = 500;

    private final ChronicleQueueService queueService;
    private final OrderRepository orderRepository;
    private ExcerptTailer namedTailer;

    @PostConstruct
    public void init() {
        namedTailer = queueService.createNamedTailer("order-projector");
    }

    @Scheduled(fixedDelay = 200)
    public void flush() {
        Map<UUID, Order> batchToSave = new HashMap<>();
        long startIndex = namedTailer.index();
        int reads = 0;

        while (reads < BATCH_SIZE) {
            try (DocumentContext dc = namedTailer.readingDocument()) {
                if (!dc.isPresent()) {
                    break;
                }

                String eventType = dc.wire().read("eventType").text();
                if (Objects.equals(eventType, EventType.OrderAccepted.name())) {
                    OrderAccepted event = dc.wire().read("event").typedMarshallable();
                    if (event != null) {
                        batchToSave.put(event.getOrderId(), OrderMapper.fromEvent(event));
                    }
                } else if (EventType.TradeExecuted.name().equals(eventType)) {
                    TradeExecuted tradedEvent = dc.wire().read("event").typedMarshallable();
                    if (tradedEvent != null) {
                        applyTradeToOrder(batchToSave, tradedEvent.getBuyOrderId(), tradedEvent);
                        applyTradeToOrder(batchToSave, tradedEvent.getSellOrderId(), tradedEvent);
                    }
                } else if (EventType.OrderCancelled.name().equals(eventType)) {
                    OrderCancelled cancelled = dc.wire().read("event").typedMarshallable();
                    if (cancelled != null) {
                        applyCancel(batchToSave, cancelled);
                    }
                } else {
                    dc.wire().read("event").typedMarshallable();
                }

                reads++;
            }

        }

        if (!batchToSave.isEmpty()) {
            try {
                orderRepository.saveAll(batchToSave.values());
            } catch (Exception e) {
                log.error("Failed to project {} orders", batchToSave.size(), e);
                namedTailer.moveToIndex(startIndex);
            }
        }
    }

    private void applyTradeToOrder(Map<UUID, Order> batch, UUID orderId, TradeExecuted trade) {
        if (orderId == null) return;

        Order order = batch.get(orderId);
        if (order == null) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
        }

        BigDecimal qty = trade.getExecutedQuantity();
        order.setFilledQuantity(order.getFilledQuantity().add(qty));
        order.setRemainingQuantity(order.getRemainingQuantity().subtract(qty));

        if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus(OrderStatus.FILLED);
            order.setRemainingQuantity(BigDecimal.ZERO);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }

        batch.put(orderId, order);
    }

    private void applyCancel(Map<UUID, Order> batch, OrderCancelled event) {
        Order order = batch.get(event.getOrderId());
        if (order == null) {
            order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));
        }
        order.setStatus(OrderStatus.CANCELED);
        batch.put(order.getId(), order);
    }

}
