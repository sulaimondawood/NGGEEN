package com.dawood.nggeen.account.api.rest.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String asset,
        BigDecimal available,
        BigDecimal reserved
) {}