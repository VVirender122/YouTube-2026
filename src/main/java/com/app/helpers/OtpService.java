package com.app.helpers;

import java.security.SecureRandom;

public final class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_MINUTES = 5;

    private OtpService() {
    }

    public static String generate() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static long expiryMillis() {
        return System.currentTimeMillis() + OTP_MINUTES * 60_000L;
    }

    public static boolean isValid(String expected, String entered, long expiry) {
        return expected != null
                && entered != null
                && System.currentTimeMillis() <= expiry
                && expected.equals(entered.trim());
    }
}
