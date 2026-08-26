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
    @DisplayName("Deve validar com sucesso uma assinatura HMAC-SHA1 legítima do YouTube")
    void shouldValidateLegitimateSignature() throws Exception {
        String xmlPayload = "<feed><entry><yt:videoId>abc123xyz</yt:videoId></entry></feed>";
        byte[] payloadBytes = xmlPayload.getBytes(StandardCharsets.UTF_8);

        String signatureHex = calculateHmacSha1Hex(payloadBytes, TEST_SECRET);
        String signatureHeader = "sha1=" + signatureHex;

        boolean isValid = validator.isValid(payloadBytes, signatureHeader);

        assertTrue(isValid, "A assinatura legítima deveria ter sido validada com sucesso");
    }

    @Test
    @DisplayName("Deve rejeitar se o payload foi alterado após a assinatura (ataque de adulteração)")
    void shouldRejectTamperedPayload() throws Exception {
        String originalXml = "<feed><entry><yt:videoId>abc123xyz</yt:videoId></entry></feed>";
        String tamperedXml = "<feed><entry><yt:videoId>FAKE_VIDEO</yt:videoId></entry></feed>";

        byte[] originalBytes = originalXml.getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBytes = tamperedXml.getBytes(StandardCharsets.UTF_8);

        // Assinatura gerada sobre o original, mas enviada com payload adulterado
        String signatureHex = calculateHmacSha1Hex(originalBytes, TEST_SECRET);
        String signatureHeader = "sha1=" + signatureHex;

        boolean isValid = validator.isValid(tamperedBytes, signatureHeader);

        assertFalse(isValid, "Payload adulterado deve ser rejeitado pelo validador");
    }

    @Test
    @DisplayName("Deve rejeitar assinatura assinada com segredo incorreto")
    void shouldRejectWrongSecret() throws Exception {
        String xmlPayload = "<feed><entry><yt:videoId>abc123xyz</yt:videoId></entry></feed>";
        byte[] payloadBytes = xmlPayload.getBytes(StandardCharsets.UTF_8);

        String signatureWithWrongSecret = calculateHmacSha1Hex(payloadBytes, "wrong-secret-key");
        String signatureHeader = "sha1=" + signatureWithWrongSecret;

        boolean isValid = validator.isValid(payloadBytes, signatureHeader);

        assertFalse(isValid, "Assinatura gerada com segredo incorreto deve ser rejeitada");
    }

    @Test
    @DisplayName("Deve rejeitar cabeçalho de assinatura nulo ou mal formatado")
    void shouldRejectMalformedHeader() {
        byte[] payloadBytes = "test".getBytes(StandardCharsets.UTF_8);

        assertFalse(validator.isValid(payloadBytes, null), "Cabeçalho nulo deve ser rejeitado");
        assertFalse(validator.isValid(payloadBytes, ""), "Cabeçalho vazio deve ser rejeitado");
        assertFalse(validator.isValid(payloadBytes, "invalid_prefix_hash"), "Cabeçalho sem prefixo sha1= deve ser rejeitado");
        assertFalse(validator.isValid(null, "sha1=123456"), "Payload nulo deve ser rejeitado");
    }

    private String calculateHmacSha1Hex(byte[] data, String secret) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data);
        return HexFormat.of().formatHex(hash);
    }
}
