package com.dawood.nggeen.account.model;

import com.dawood.nggeen.account.exception.InsufficientBalanceException;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "account_balances",
        indexes = {@Index(name = "idx_balance_accound_id", columnList = "account_id")})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AccountBalance extends MetaData {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String asset;

    @Column(nullable = false)
    private BigDecimal available = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal reserved = BigDecimal.ZERO;

    public AccountBalance(UUID accountId, String asset, BigDecimal available, BigDecimal reserved) {
        this.accountId = accountId;
        this.asset = asset;
        this.available = available;
        this.reserved = reserved;
    }

    public void reserve(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (available.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    ErrorCode.INSUFFICIENT_FUNDS,
                    String.format("Insufficient %s balance. Available: %s, Requested: %s", asset, available, amount),
                    HttpStatus.BAD_REQUEST
            );
        }

        available = available.subtract(amount);
        reserved = reserved.add(amount);

    }

    public void release(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (reserved.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    String.format("Cannot release more than reserved. Reserved: %s, Release amount: %s", reserved, amount),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        available = available.add(amount);
        reserved = reserved.subtract(amount);
    }

    public void settleDeduction(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (reserved.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    String.format("Cannot settle more than reserved balance. Reserved: %s, Settle amount: %s", reserved, amount),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        reserved = reserved.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount. Amount must be strictly positive");
        }
    }
}
