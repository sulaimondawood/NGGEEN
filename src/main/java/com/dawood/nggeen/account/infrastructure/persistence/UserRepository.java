package com.dawood.nggeen.account.infrastructure.persistence;

import com.dawood.nggeen.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);
}
