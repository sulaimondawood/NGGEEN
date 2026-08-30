package com.dawood.nggeen.shared.infrastructure.persistence;

import com.dawood.nggeen.account.model.Account;

import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUserIdAndAccountTypeAndStatus(UUID userId, AccountType type, AccountStatus status);
}
