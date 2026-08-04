package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.shared.model.MetaData;
import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderStatus;
import com.dawood.nggeen.trade.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Order extends MetaData {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
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
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_NEW;

    public boolean isFilled(){
        return quantity.compareTo(filledQuantity) <= 0;
    }

    public void fillQuantity(BigDecimal fillQty){
        if(fillQty == null || fillQty.compareTo(BigDecimal.ZERO) <=0){
            throw new IllegalArgumentException("Fill amount must be greater than zero");
        }

        if(remainingQuantity == null){
            throw new IllegalStateException("remainingQuantity not initialized");
        }

        if(fillQty.compareTo(remainingQuantity) >0){
            throw new IllegalArgumentException("Fill amount exceeds order remaining quantity");
        }

        filledQuantity = filledQuantity.add(fillQty);
        remainingQuantity = remainingQuantity.subtract(fillQty);
        status = isFilled()? OrderStatus.FILLED: OrderStatus.PARTIALLY_FILLED;
    }

    public boolean isMatchablePrice( Order restingOrder){
        OrderSide orderSide =this.orderSide;
        BigDecimal incomingPrice = this.getPrice();
        BigDecimal restingPrice = restingOrder.getPrice();
        if (restingPrice == null) {
            return false;
        }

        if(orderSide == OrderSide.BUY){
            return incomingPrice.compareTo(restingPrice) >= 0;
        }
        return incomingPrice.compareTo(restingPrice) <= 0;
    }

}
