package com.dawood.nggeen.trade.api.rest.dto;

import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderType;
import jakarta.validation.constraints.AssertTrue;
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

    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00000001", message = "Quote quantity must be greater than zero")
    private BigDecimal quoteQty;

    @AssertTrue(message = "LIMIT orders require both 'price' and 'quantity'")
    public boolean isValidLimitOrder() {
        if (orderType == OrderType.LIMIT) {
            return price != null && quantity != null;
        }
        return true;
    }

    @AssertTrue(message = "Provide either 'quantity' or 'quoteQuantity', not both or neither")
    public boolean isValidQtySpecified() {
        if (orderType == OrderType.MARKET) {
              boolean hasQty = quantity != null;
              boolean hasQuoteQty = quoteQty != null;
              return hasQty != hasQuoteQty;
        }
        return true;
    }
}
