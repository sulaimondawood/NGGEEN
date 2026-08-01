package com.dawood.nggeen.domain;


import com.dawood.nggeen.domain.enums.OrderStatus;
import com.dawood.nggeen.domain.enums.OrderType;
import com.dawood.nggeen.domain.enums.Side;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Order {
    private UUID id;

    private UUID orderId;

    private Side side;

    private OrderType type;

    private BigDecimal price;

    private BigDecimal quantity;

    private BigDecimal filledQuantity = BigDecimal.ZERO;

    private OrderStatus status = OrderStatus.NEW;

    private LocalDateTime timestamp;

    private String userId;

    public BigDecimal getRemainingQuantity(){
        return quantity.subtract(filledQuantity);
    }

    public boolean isFilled(){
        return getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0;
    }

    public void fill(BigDecimal quantityToFill){
        if(quantityToFill == null || quantityToFill.compareTo(BigDecimal.ZERO) <=0) return;

        BigDecimal newQuantityFill = filledQuantity.add(quantityToFill);

        filledQuantity = newQuantityFill.compareTo(quantity)>0?quantityToFill:newQuantityFill;

        if(isFilled()){
          status=OrderStatus.FILLED;
        }else {
            status=OrderStatus.PARTIALLY_FILLED;
        }
    }

    public void cancel(String user){
        status = OrderStatus.CANCELLED;
        timestamp=LocalDateTime.now();
        userId = user;
    }
}
