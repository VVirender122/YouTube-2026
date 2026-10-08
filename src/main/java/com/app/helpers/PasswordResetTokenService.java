package com.app.helpers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordResetTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int TOKEN_BYTES = 32;

    private static final long EXPIRY_MILLIS =
            15L * 60L * 1000L;

    private PasswordResetTokenService() {
    }

    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static String hashToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", e);
        }
    }

    public static long expiryMillis() {
        return System.currentTimeMillis() + EXPIRY_MILLIS;
    }

    public static long lifetimeMinutes() {
        return 15;
    }
}