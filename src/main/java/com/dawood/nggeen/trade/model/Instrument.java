package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.enums.InstrumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Table(name = "instruments")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Instrument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String baseAsset;

    @Column(nullable = false)
    private String quoteAsset;

    @Column(nullable = false)
    private BigDecimal tickSize;

    @Column(nullable = false)
    private BigDecimal stepSize;

    @Column(nullable = false)
    private BigDecimal minQuantity;

    @Column(nullable = false)
    private BigDecimal maxQuantity;

    @Column(nullable = false)
    private BigDecimal minQuoteAmount;

    @Column(nullable = false)
    private BigDecimal maxQuoteAmount;

    @Column(nullable = false)
    private int pricePrecision;

    @Column(nullable = false)
    private int quantityPrecision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstrumentStatus status;

}
