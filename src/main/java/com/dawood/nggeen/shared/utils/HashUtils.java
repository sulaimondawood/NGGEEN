package com.dawood.nggeen.shared.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashUtils {
    private static final String HASH_ALGORITHM = "SHA-256";

    public static String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize hashing algorithm: " + HASH_ALGORITHM, e);
        }
    }

    public static boolean verifyToken(String rawToken, String hashedToken) {
        if (rawToken == null || hashedToken == null) {
            return false;
        }
        String currentHash = hashToken(rawToken);

        return MessageDigest.isEqual(
                currentHash.getBytes(StandardCharsets.UTF_8),
                hashedToken.getBytes(StandardCharsets.UTF_8));
    }
}
