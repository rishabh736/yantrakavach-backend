package com.yantrakavach.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generates and hashes device API keys. Keys are never stored in plain text — only their SHA-256 hash is. */
public final class DeviceKeys {

    private static final SecureRandom RANDOM = new SecureRandom();

    private DeviceKeys() {
    }

    /** A random, URL-safe device key such as "yk_8gq2m1z...". Shown to the operator exactly once. */
    public static String generateRaw() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "yk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JDK spec; this is unreachable in practice.
            throw new IllegalStateException(e);
        }
    }
}
