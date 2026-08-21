package com.dawood.nggeen.account.infrastructure.persistence;

import com.dawood.nggeen.account.model.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
}
