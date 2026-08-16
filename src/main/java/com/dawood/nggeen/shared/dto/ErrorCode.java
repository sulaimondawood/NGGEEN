package com.dawood.nggeen.shared.dto;

public enum ErrorCode {
    BAD_REQUEST("BAD_REQUEST"),
    ORDER_EXECUTION_FAILED("ORDER_EXECUTION_FAILED"),
    INSUFFICIENT_LIQUIDITY("INSUFFICIENT_LIQUIDITY"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR");

    private String code;

    ErrorCode(String code) {
        this.code = code;
    }
}
