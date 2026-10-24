package com.yantrakavach.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, dependency-free JWT (HMAC-SHA256) implementation. Deliberately avoids pulling in a third-party
 * JWT library so this project has one fewer version to keep in sync; the algorithm is a handful of lines
 * of standard javax.crypto and is easy to audit.
 */
@Component
public class JwtUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long expiryMinutes;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiry-minutes:720}") long expiryMinutes) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("app.jwt.secret must be at least 16 characters long");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expiryMinutes = expiryMinutes;
    }

    public String generateToken(String subjectEmail, String role, String name) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long now = Instant.now().getEpochSecond();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", subjectEmail);
            payload.put("role", role);
            payload.put("name", name);
            payload.put("iat", now);
            payload.put("exp", now + expiryMinutes * 60);

            String headerPart = ENC.encodeToString(MAPPER.writeValueAsBytes(header));
            String payloadPart = ENC.encodeToString(MAPPER.writeValueAsBytes(payload));
            String signingInput = headerPart + "." + payloadPart;
            String signature = ENC.encodeToString(hmac(signingInput));
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate token", e);
        }
    }

    /** Returns the verified claims, or null if the token is malformed, mis-signed or expired. */
    public Claims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = hmac(signingInput);
            byte[] actual = DEC.decode(parts[2]);
            if (!constantTimeEquals(expected, actual)) return null;

            Map<?, ?> payload = MAPPER.readValue(DEC.decode(parts[1]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) return null;

            return new Claims(
                    String.valueOf(payload.get("sub")),
                    String.valueOf(payload.get("role")),
                    String.valueOf(payload.get("name"))
            );
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] hmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
        return result == 0;
    }

    public record Claims(String email, String role, String name) {
    }
}
