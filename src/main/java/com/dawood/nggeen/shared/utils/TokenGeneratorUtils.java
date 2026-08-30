package com.dawood.nggeen.shared.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGeneratorUtils {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
