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
        indexes = {@Index(name = "idx_balance_account_id", columnList = "account_id")},
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_balance_account_asset", columnNames = {"account_id", "asset"})
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AccountBalance extends MetaData {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Column(nullable = false)
    private String asset;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal available = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal reserved = BigDecimal.ZERO;

    public AccountBalance(Account account, String asset, BigDecimal available, BigDecimal reserved) {
        this.account = account;
        this.asset = asset;
        this.available = available;
        this.reserved = reserved;
    }

    public void lockFunds(BigDecimal amount) {
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

    public void releaseLockedFunds(BigDecimal amount) {
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

    public void credit(BigDecimal amount) {
        validatePositiveAmount(amount);
        available = available.add(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount. Amount must be strictly positive");
        }
    }
}
