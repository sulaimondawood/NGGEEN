package com.dawood.nggeen.account.api.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DemoDepositRequest(
        @NotBlank(message = "Asset is required") String asset,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00000001", message = "Amount must be greater than zero")
        BigDecimal amount
) {}