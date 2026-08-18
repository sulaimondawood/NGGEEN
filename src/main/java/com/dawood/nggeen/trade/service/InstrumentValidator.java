package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.trade.exception.InvalidOrderQuantityException;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.Order;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InstrumentValidator {
    public void validate(Order incomingOrder, Instrument instrument) {
        BigDecimal qty = incomingOrder.getQuantity();
        BigDecimal price = incomingOrder.getPrice();

        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderQuantityException(ErrorCode.BAD_REQUEST,
                    "Invalid order quantity", HttpStatus.BAD_REQUEST);
        }

        if(price == null || price.compareTo(BigDecimal.ZERO) < 0){
            throw new InvalidOrderQuantityException(ErrorCode.BAD_REQUEST,
                    "Invalid order quantity", HttpStatus.BAD_REQUEST);
        }
    }
}
