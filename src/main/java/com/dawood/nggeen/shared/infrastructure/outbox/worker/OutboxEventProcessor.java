package com.dawood.nggeen.shared.infrastructure.outbox.worker;

import com.dawood.nggeen.shared.infrastructure.outbox.model.OutboxEvent;
import com.dawood.nggeen.shared.infrastructure.outbox.persistence.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${nggeen.outbox.poller.batch-size}")
    private int batchSize;

    @Transactional
    public void processBatch() {
        List<OutboxEvent> eventLists = outboxRepository.findPendingBatchEventsForProcessing(Instant.now(), batchSize);

        if (eventLists.isEmpty()) {
            return;
        }

        for (OutboxEvent event : eventLists) {
            String exchange = event.getDestinationExchange();
            String routingKey = event.getRoutingKey();

            try {
                rabbitTemplate.convertAndSend(
                        exchange,
                        routingKey,
                        event.getPayload(),
                        message -> {
                            MessageProperties props = message.getMessageProperties();
                            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                            props.setMessageId(event.getId().toString());
                            props.setHeader("eventType", event.getEventType().name());
                            return message;
                        }
                );
                event.markProcessed();
                log.debug("Successfully relayed outbox event: {}", event.getId());

            } catch (Exception e) {
                log.error("Outbox publish failed id={}, type={}", event.getId(), event.getEventType(), e);
                long backOffSeconds = (long) Math.pow(2, event.getRetryCount());
                event.markFailed(e.getMessage(), backOffSeconds);
            }
        }
    }

}
