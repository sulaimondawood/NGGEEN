package com.dawood.nggeen.trade.api.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelOrderRequest(
        @NotNull(message = "Order id is missing") UUID orderId,
        @NotNull(message = "Instrument symbol is missing") String symbol
) {
}
