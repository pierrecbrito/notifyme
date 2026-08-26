package com.notifyme.webhook.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cryptographic HMAC-SHA1 Signature Validator.
 * 
 * Enforces Non-Functional Security Requirements:
 * - Validates whether the XML payload legitimately originated from YouTube WebSub Hub.
 * - Uses MessageDigest.isEqual for constant-time comparison (mitigating timing attacks).
 */
@Slf4j
@Component
public class HmacSignatureValidator {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String SIGNATURE_PREFIX = "sha1=";

    private final String secretKey;

    public HmacSignatureValidator(@Value("${notifyme.websub.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Validates if the X-Hub-Signature header matches the received raw payload.
     * 
     * @param rawPayload Received raw HTTP request body bytes.
     * @param signatureHeader Value of the "X-Hub-Signature" header (e.g., "sha1=4a5b...").
     * @return true if the signature is authentic; false otherwise.
     */
    public boolean isValid(byte[] rawPayload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Missing or malformed signature in X-Hub-Signature header");
            return false;
        }

        if (rawPayload == null || rawPayload.length == 0) {
            log.warn("Received payload is empty");
            return false;
        }

        try {
            String expectedHashHex = signatureHeader.substring(SIGNATURE_PREFIX.length()).trim();
            byte[] expectedHashBytes = HexFormat.of().parseHex(expectedHashHex);

            byte[] calculatedHashBytes = calculateHmacSha1(rawPayload, secretKey);

            // Constant-time comparison to prevent side-channel timing attacks
            boolean matches = MessageDigest.isEqual(expectedHashBytes, calculatedHashBytes);
            if (!matches) {
                log.warn("Calculated HMAC signature does not match the signature provided by YouTube");
            }
            return matches;
        } catch (Exception e) {
            log.error("Error validating HMAC signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Calculates the HMAC-SHA1 hash over raw bytes using the shared secret key.
     */
    private byte[] calculateHmacSha1(byte[] data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }
}
