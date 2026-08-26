package com.notifyme.webhook.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacSignatureValidatorTest {

    private static final String TEST_SECRET = "notifyme-test-secret-123";
    private HmacSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HmacSignatureValidator(TEST_SECRET);
    }

    @Test
    @DisplayName("Should successfully validate a legitimate YouTube HMAC-SHA1 signature")
    void shouldValidateLegitimateSignature() throws Exception {
        String xmlPayload = "<feed><entry><yt:videoId>abc123xyz</yt:videoId></entry></feed>";
        byte[] payloadBytes = xmlPayload.getBytes(StandardCharsets.UTF_8);

        String signatureHex = calculateHmacSha1Hex(payloadBytes, TEST_SECRET);
        String signatureHeader = "sha1=" + signatureHex;

        boolean isValid = validator.isValid(payloadBytes, signatureHeader);

        assertTrue(isValid, "Legitimate signature should be validated successfully");
    }

    @Test
    @DisplayName("Should reject tampered payloads when signature does not match (tampering attack)")
    void shouldRejectTamperedPayload() throws Exception {
        String originalXml = "<feed><entry><yt:videoId>abc123xyz</yt:videoId></entry></feed>";
        String tamperedXml = "<feed><entry><yt:videoId>FAKE_VIDEO</yt:videoId></entry></feed>";

        byte[] originalBytes = originalXml.getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBytes = tamperedXml.getBytes(StandardCharsets.UTF_8);

        // Signature generated over original payload, sent with tampered payload
        String signatureHex = calculateHmacSha1Hex(originalBytes, TEST_SECRET);
        String signatureHeader = "sha1=" + signatureHex;

        boolean isValid = validator.isValid(tamperedBytes, signatureHeader);

        assertFalse(isValid, "Tampered payload must be rejected by validator");
    }

    @Test
    @DisplayName("Should reject signatures created with an incorrect secret key")
    void shouldRejectWrongSecret() throws Exception {
        String xmlPayload = "<feed><entry><yt:videoId>abc123xyz</yt:videoId></entry></feed>";
        byte[] payloadBytes = xmlPayload.getBytes(StandardCharsets.UTF_8);

        String signatureWithWrongSecret = calculateHmacSha1Hex(payloadBytes, "wrong-secret-key");
        String signatureHeader = "sha1=" + signatureWithWrongSecret;

        boolean isValid = validator.isValid(payloadBytes, signatureHeader);

        assertFalse(isValid, "Signature generated with wrong secret must be rejected");
    }

    @Test
    @DisplayName("Should reject null, empty, or malformed signature headers")
    void shouldRejectMalformedHeader() {
        byte[] payloadBytes = "test".getBytes(StandardCharsets.UTF_8);

        assertFalse(validator.isValid(payloadBytes, null), "Null header must be rejected");
        assertFalse(validator.isValid(payloadBytes, ""), "Empty header must be rejected");
        assertFalse(validator.isValid(payloadBytes, "invalid_prefix_hash"), "Header without sha1= prefix must be rejected");
        assertFalse(validator.isValid(null, "sha1=123456"), "Null payload must be rejected");
    }

    private String calculateHmacSha1Hex(byte[] data, String secret) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data);
        return HexFormat.of().formatHex(hash);
    }
}
