package com.dawood.nggeen.trade.api.rest.dto;

import com.dawood.nggeen.trade.enums.OrderSide;
import com.dawood.nggeen.trade.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlaceOrderRequest {
    @NotNull(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Order type is required (e.g. LIMIT, MARKET)")
    private OrderType orderType;

    @NotNull(message = "Order side is required (e.g. BUY, SELL)")
    private OrderSide orderSide;

    @DecimalMin(value = "0.00000001", message = "Price must be greater than zero")
    private BigDecimal price;

    @DecimalMin(value = "0.00000001", message = "Stop price must be greater than zero")
    private BigDecimal stopPrice;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;


}
