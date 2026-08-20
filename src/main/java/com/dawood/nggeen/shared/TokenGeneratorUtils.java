package com.dawood.nggeen.shared;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGeneratorUtils {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
