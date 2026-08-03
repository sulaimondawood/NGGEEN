package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.shared.model.MetaData;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order extends MetaData {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Instrument instrument;

    @Column(nullable = false, unique = true)
    private long sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide orderSide;

    private BigDecimal price;
    private BigDecimal stopPrice;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

}
