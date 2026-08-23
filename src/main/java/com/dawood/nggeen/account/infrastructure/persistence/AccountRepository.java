package com.dawood.nggeen.account.infrastructure.persistence;

import com.dawood.nggeen.account.model.Account;

import com.dawood.nggeen.account.model.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUserIdAndAccountType(UUID userId, AccountType type);
}
