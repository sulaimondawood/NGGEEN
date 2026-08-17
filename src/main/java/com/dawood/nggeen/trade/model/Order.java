package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.shared.model.MetaData;
import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.event.OrderAccepted;
import com.dawood.nggeen.trade.event.OrderCancelled;
import com.dawood.nggeen.trade.model.enums.CancelReason;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderStatus;
import com.dawood.nggeen.trade.model.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;
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

    @Column(nullable = false)
    private String symbol;

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

    @JsonIgnore
    public boolean isFilled() {
        return quantity.compareTo(filledQuantity) <= 0;
    }

    public void fillQuantity(BigDecimal fillQty) {
        if (fillQty == null || fillQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fill amount must be greater than zero");
        }

        if (remainingQuantity == null) {
            throw new IllegalStateException("Remaining Quantity is not initialized");
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

    public DomainEvent markAccepted(long seq) {
        this.status = OrderStatus.NEW;
        return new OrderAccepted(
                id,
                seq,
                symbol,
                price,
                stopPrice,
                quantity,
                orderSide,
                orderType);
    }

    public DomainEvent markCancelled(long seq,
                                     BigDecimal quantityCancelled,
                                     CancelReason reason,
                                     OrderStatus status) {
        this.status = status;
        return new OrderCancelled(
                id,
                seq,
                symbol,
                quantityCancelled,
                reason
        );
    }

    public static Order buildOrderFromEvent(OrderAccepted event) {
        if(event == null){
            throw new IllegalArgumentException("OrderAccepted event must not be null");
        }
        return Order.builder()
                .id(event.getOrderId())
                .symbol(event.symbol())
                .orderType(event.getOrderType())
                .orderSide(event.getOrderSide())
                .price(event.getPrice())
                .stopPrice(event.getStopPrice())
                .quantity(event.getQuantity())
                .remainingQuantity(event.getQuantity())
                .status(OrderStatus.NEW)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
