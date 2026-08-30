package com.dawood.nggeen.shared.infrastructure.persistence;

import com.dawood.nggeen.account.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
}
