package com.dawood.nggeen.shared.infrastructure.persistence;

import com.dawood.nggeen.account.model.AccountBalance;
import com.dawood.nggeen.account.model.enums.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountBalance> findByAccountIdAndAsset(UUID accountId, String asset);
}
