package com.app.helpers;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing using the JDK only.
 * Stored format: PBKDF2WithHmacSHA256$iterations$salt$hash
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS);
        return ALGORITHM + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        try {
            String[] parts = storedHash.split("\\$", 4);
            if (parts.length != 4 || !ALGORITHM.equals(parts[0])) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);

            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
            try {
                return SecretKeyFactory.getInstance(ALGORITHM)
                        .generateSecret(spec)
                        .getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable.", e);
        }
    }
}
