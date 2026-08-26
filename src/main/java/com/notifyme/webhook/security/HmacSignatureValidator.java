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
 * Validador Criptográfico de Assinatura HMAC-SHA1.
 * 
 * Garante o Requisito Não-Funcional de Segurança:
 * - Valida se o payload XML foi legitimamente emitido pelo YouTube WebSub Hub.
 * - Utiliza MessageDigest.isEqual para comparação em tempo constante (protege contra Timing Attacks).
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
     * Valida se o cabeçalho X-Hub-Signature corresponde ao payload recebido.
     * 
     * @param rawPayload Corpo bruto (bytes) da requisição HTTP recebida.
     * @param signatureHeader Valor do cabeçalho "X-Hub-Signature" (ex: "sha1=4a5b...").
     * @return true se a assinatura for válida e autêntica; false caso contrário.
     */
    public boolean isValid(byte[] rawPayload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Assinatura ausente ou com formato inválido no cabeçalho X-Hub-Signature");
            return false;
        }

        if (rawPayload == null || rawPayload.length == 0) {
            log.warn("Payload recebido está vazio");
            return false;
        }

        try {
            String expectedHashHex = signatureHeader.substring(SIGNATURE_PREFIX.length()).trim();
            byte[] expectedHashBytes = HexFormat.of().parseHex(expectedHashHex);

            byte[] calculatedHashBytes = calculateHmacSha1(rawPayload, secretKey);

            // Comparação em tempo constante para evitar ataques de temporização (Timing Attacks)
            boolean matches = MessageDigest.isEqual(expectedHashBytes, calculatedHashBytes);
            if (!matches) {
                log.warn("Assinatura HMAC calculada não confere com a assinatura enviada pelo YouTube");
            }
            return matches;
        } catch (Exception e) {
            log.error("Erro ao validar assinatura HMAC: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Calcula o hash HMAC-SHA1 sobre os bytes do payload usando a chave secreta.
     */
    private byte[] calculateHmacSha1(byte[] data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }
}
