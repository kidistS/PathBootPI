package com.pathboot.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for generating stable cache keys.
 *
 * <p>Used via SpEL in {@code @Cacheable} annotations to keep key lengths
 * bounded regardless of the length of the user's question.</p>
 */
public final class CacheKeyUtil {

    private CacheKeyUtil() { /* static utility class */ }

    /**
     * Returns the SHA-256 hex digest of the given text.
     * Falls back to {@code String.valueOf(text.hashCode())} if SHA-256 is unavailable
     * (this should never happen on a Java 8+ JVM).
     *
     * @param text input to hash
     * @return 64-character lowercase hex string, or a decimal hashCode string on fallback
     */
    public static String hashQuestion(String text) {
        if (text == null) {
            return "null";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the Java spec – this branch is unreachable in practice.
            return String.valueOf(text.hashCode());
        }
    }
}

