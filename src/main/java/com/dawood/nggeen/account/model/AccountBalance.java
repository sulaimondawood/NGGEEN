package com.dawood.nggeen.account.model;

import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "account_balances")
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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount. Reserve amount must be strictly positive");
        }

    }
}
