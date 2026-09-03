package com.dawood.nggeen.shared.infrastructure.outbox.model;

import com.dawood.nggeen.shared.infrastructure.outbox.model.enums.OutboxEventType;
import com.dawood.nggeen.shared.infrastructure.outbox.model.enums.OutboxStatus;
import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_at, created_at")
        })
@Entity
public class OutboxEvent extends MetaData {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    private Instant processedAt;

    @Column(nullable = false)
    @Builder.Default
    private Instant nextRetryAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String destinationExchange;

    @Column(nullable = false)
    private String routingKey;

    public static OutboxEvent of(OutboxEventType eventType,
                                 String payload,
                                 String destinationExchange,
                                 String routingKey) {
        return OutboxEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .nextRetryAt(Instant.now())
                .retryCount(0)
                .maxRetries(5)
                .destinationExchange(destinationExchange)
                .routingKey(routingKey)
                .build();
    }

    public void markProcessed() {
        status = OutboxStatus.PROCESSED;
        processedAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error, long backoffSeconds) {
        retryCount++;
        lastError = error != null && error.length() > 1024 ? error.substring(0, 1024) : error;
        if (retryCount >= maxRetries) {
            status = OutboxStatus.FAILED;
            processedAt = Instant.now();
        } else {
            nextRetryAt = Instant.now().plusSeconds(backoffSeconds);
        }
    }

}
