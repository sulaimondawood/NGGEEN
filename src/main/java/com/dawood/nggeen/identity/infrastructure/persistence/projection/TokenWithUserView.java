package com.dawood.nggeen.identity.infrastructure.persistence.projection;

import java.time.Instant;
import java.util.UUID;

public interface TokenWithUserView {
    UUID tokenId();

    String token();

    Instant getExpiresAt();

    Instant getUsedAt();

    UUID getUserId();

    String getEmail();

    String getUserStatus();

}
