package com.dawood.nggeen.trade.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CancelOrderRequest(
        @NotBlank(message = "Order id is missing") UUID orderId,
        @NotBlank(message = "Instrument symbol is missing") String symbol
) {
}
