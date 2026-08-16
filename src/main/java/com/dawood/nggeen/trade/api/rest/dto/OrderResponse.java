package com.dawood.nggeen.trade.api.rest.dto;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String symbol,
        OrderType orderType,
        OrderSide orderSide,
        BigDecimal price,
        BigDecimal stopPrice,
        BigDecimal quantity
) {
}
