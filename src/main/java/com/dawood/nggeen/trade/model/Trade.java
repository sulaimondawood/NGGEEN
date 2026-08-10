package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Trade extends MetaData {
    @Id
    @Column(nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private long sequenceNo;

    @Column(nullable = false, updatable = false)
    private String symbol;

    @Column(nullable = false, updatable = false)
    private UUID sellOrderId;

    @Column(nullable = false, updatable = false)
    private UUID buyOrderId;

    @Column(nullable = false, updatable = false)
    private BigDecimal price;

    @Column(nullable = false, updatable = false)
    private BigDecimal quantity;

    @Column(nullable = false, updatable = false)
    private Instant executedAt;
}
