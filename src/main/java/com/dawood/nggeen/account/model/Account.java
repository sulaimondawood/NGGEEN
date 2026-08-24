package com.dawood.nggeen.account.model;

import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "user_accounts",
        indexes = {@Index(name = "idx_account_userId_type", columnList = "user_id, account_type")}
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Account extends MetaData {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.SPOT;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;
}
