package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.shared.model.MetaData;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.enums.OrderType;
import com.dawood.nggeen.trade.event.AggregateEvent;
import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.event.OrderCancelled;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Order extends MetaData {
    @Id
    @Column(nullable = false, updatable = false, unique = true)
    private UUID id;

    private String symbol;

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
    @Builder.Default
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_NEW;

    @Transient
    @Getter(value = AccessLevel.NONE)
    @Builder.Default
    private AggregateEvent events = new AggregateEvent();

    public boolean isFilled() {
        return quantity.compareTo(filledQuantity) <= 0;
    }

    public void fillQuantity(BigDecimal fillQty) {
        if (fillQty == null || fillQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fill amount must be greater than zero");
        }

        if (remainingQuantity == null) {
            throw new IllegalStateException("remainingQuantity not initialized");
        }

        if (fillQty.compareTo(remainingQuantity) > 0) {
            throw new IllegalArgumentException("Fill amount exceeds order remaining quantity");
        }

        filledQuantity = filledQuantity.add(fillQty);
        remainingQuantity = remainingQuantity.subtract(fillQty);
        status = isFilled() ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    public boolean isMatchablePrice(Order restingOrder) {
        BigDecimal incomingPrice = this.getPrice();
        BigDecimal restingPrice = restingOrder.getPrice();
        if (restingPrice == null || incomingPrice == null) {
            return false;
        }

        if (this.orderSide == OrderSide.BUY) {
            return incomingPrice.compareTo(restingPrice) >= 0;
        }
        return incomingPrice.compareTo(restingPrice) <= 0;
    }

    public OrderAccepted markAccepted(long seq) {
        this.status = OrderStatus.NEW;
        OrderAccepted event = new OrderAccepted(
                id,
                seq,
                symbol,
                price,
                stopPrice,
                quantity,
                orderSide,
                orderType);

        events.registerEvent(event);
        return event;
    }

    public OrderCancelled markCancelled(){
        status = OrderStatus.CANCELED;
        OrderCancelled event = new OrderCancelled(

        )
    }

}
