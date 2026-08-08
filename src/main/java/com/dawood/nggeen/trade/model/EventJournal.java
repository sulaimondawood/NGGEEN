package com.dawood.nggeen.trade.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String instrumentId;
    @Column(nullable = false, updatable = false)
    private long sequenceNo;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    public EventJournal(String instrumentId, long sequenceNo, String eventType,
                              String payloadJson, Instant occurredAt) {
        this.instrumentId = instrumentId;
        this.sequenceNo = sequenceNo;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.occurredAt = occurredAt;
    }
}
