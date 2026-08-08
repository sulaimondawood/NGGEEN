package com.dawood.nggeen.trade.event;

import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class OrderAccepted{
    private UUID orderId;
    private long sequenceNo;
    private String symbol;
    private Instant timestamp;
    private OrderType orderType;
    private OrderSide orderSide;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private BigDecimal quantity;

}
