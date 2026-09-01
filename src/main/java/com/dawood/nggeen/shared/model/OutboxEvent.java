package com.dawood.nggeen.shared.model;

import com.dawood.nggeen.shared.model.enums.OutboxEventType;
import com.dawood.nggeen.shared.model.enums.OutboxStatus;
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
@Table(name = "outbox_events")
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

    @Column(nullable = false)
    private Instant processedAt;

    @Column(nullable = false)
    @Builder.Default
    private Instant nextRetryAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private int retries = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false, length = 128)
    private String destinationExchange;

    @Column(nullable = false, length = 128)
    private String routingKey;

    public void markProcessed() {
        status = OutboxStatus.PROCESSED;
       processedAt = Instant.now();
       lastError = null;
    }

    public void recordFailure(String error, int backoffSeconds) {
        retries++;
        lastError = error != null && error.length() > 1024 ? error.substring(0, 1024) : error;
        if (retries >= maxRetries) {
            status = OutboxStatus.FAILED;
            processedAt = Instant.now();
        } else {
            nextRetryAt = Instant.now().plusSeconds(backoffSeconds);
        }
    }

}
