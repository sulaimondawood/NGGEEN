package com.dawood.nggeen.account.infrastructure.persistence;

import com.dawood.nggeen.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
