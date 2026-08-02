package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.shared.model.MetaData;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
public class Order extends MetaData {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @OneToOne
    private Instrument instrument;

    private OrderType orderType;

    private OrderSide orderSide;

    private BigDecimal price;

    private BigDecimal stopPrice;

    private BigDecimal quantity;

    private BigDecimal filledQuantity;

    private BigDecimal remainingQuantity;

    private OrderStatus status;

}
