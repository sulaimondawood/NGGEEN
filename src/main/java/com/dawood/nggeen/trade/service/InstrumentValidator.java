package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.InvalidOrderException;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.enums.InstrumentStatus;
import com.dawood.nggeen.trade.model.enums.OrderType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InstrumentValidator {
    public void validate(Order incomingOrder, Instrument instrument) {
        if (!instrument.getStatus().equals(InstrumentStatus.TRADING)) {
            throw new InvalidOrderException(ErrorCode.MARKET_NOT_TRADING,
                    "Instrument is not opened for trading",
                    HttpStatus.BAD_REQUEST);
        }

        BigDecimal qty = incomingOrder.getQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException(
                    ErrorCode.INVALID_QUANTITY,
                    "Order quantity must be strictly positive",
                    HttpStatus.BAD_REQUEST);
        }

        if (qty.compareTo(instrument.getMinQuantity()) < 0) {
            throw new InvalidOrderException(ErrorCode.QUANTITY_BELOW_MINIMUM,
                    "Order quantity is below minimum allowed: " + instrument.getMinQuantity(),
                    HttpStatus.BAD_REQUEST);
        }

        if (qty.compareTo(instrument.getMaxQuantity()) > 0) {
            throw new InvalidOrderException(ErrorCode.QUANTITY_EXCEEDS_MAXIMUM,
                    "Order quantity exceeds maximum allowed: " + instrument.getMaxQuantity(),
                    HttpStatus.BAD_REQUEST);
        }

        if (qty.scale() > instrument.getQuantityPrecision()) {
            throw new InvalidOrderException(ErrorCode.QUANTITY_EXCEEDS_MAXIMUM,
                    "Order quantity exceeds allowed precision: " + instrument.getQuantityPrecision(),
                    HttpStatus.BAD_REQUEST);
        }

        if (!isMultipleOf(qty, instrument.getStepSize())) {
            throw new InvalidOrderException(ErrorCode.LOT_SIZE_STEP_MISMATCH,
                    "Quantity must be a multiple of step size: " + instrument.getStepSize(),
                    HttpStatus.BAD_REQUEST);
        }

        if (incomingOrder.getOrderType() == OrderType.LIMIT) {
            BigDecimal price = incomingOrder.getPrice();

            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOrderException(ErrorCode.INVALID_PRICE,
                        "Order price must be strictly positive",
                        HttpStatus.BAD_REQUEST);
            }

            if (price.scale() > instrument.getPricePrecision()) {
                throw new InvalidOrderException(ErrorCode.PRICE_PRECISION_EXCEEDED,
                        "Price exceeds allowed precision: " + instrument.getPricePrecision(),
                        HttpStatus.BAD_REQUEST);
            }

            if (!isMultipleOf(price, instrument.getTickSize())) {
                throw new InvalidOrderException(ErrorCode.PRICE_TICK_SIZE_MISMATCH,
                        "Price must be a multiple of tick size: " + instrument.getTickSize(),
                        HttpStatus.BAD_REQUEST);
            }

            BigDecimal notional = price.multiply(qty);
            if (instrument.getMinQuoteAmount() != null && notional.compareTo(instrument.getMinQuoteAmount()) < 0) {
                throw new InvalidOrderException(
                        ErrorCode.MIN_NOTIONAL_NOT_MET,
                        "Total order value is below minimum allowed notional: " + instrument.getMinQuoteAmount(),
                        HttpStatus.BAD_REQUEST);
            }

            if (instrument.getMaxQuoteAmount() != null && notional.compareTo(instrument.getMaxQuoteAmount()) > 0) {
                throw new InvalidOrderException(
                        ErrorCode.MAX_NOTIONAL_EXCEEDED,
                        "Total order value exceeds maximum allowed notional: " + instrument.getMaxQuoteAmount(),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private boolean isMultipleOf(BigDecimal value, BigDecimal step) {
        if (step == null || step.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return value.remainder(step).compareTo(BigDecimal.ZERO) == 0;
    }
}


