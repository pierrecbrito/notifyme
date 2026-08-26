package com.notifyme.webhook.controller;

import com.notifyme.api.dto.ApiResponse;
import com.notifyme.webhook.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de Ingestão de Webhooks do YouTube WebSub (PubSubHubbub).
 * 
 * Camada fina de entrada HTTP que delega o processamento para o WebhookService.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook/youtube")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Handshake do WebSub: O YouTube chama este endpoint via GET ao assinar um canal.
     * Deve responder 200 OK com o 'hub.challenge' como texto plano.
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleWebSubVerification(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.topic", required = false) String topic,
            @RequestParam(name = "hub.challenge", required = false) String challenge,
            @RequestParam(name = "hub.lease_seconds", required = false) String leaseSeconds
    ) {
        log.info("Recebido handshake do YouTube WebSub. Mode: {}, Topic: {}", mode, topic);

        if (challenge == null || challenge.isBlank()) {
            log.warn("Handshake recebido sem hub.challenge");
            return ResponseEntity.badRequest().body("hub.challenge ausente");
        }

        return ResponseEntity.ok(challenge);
    }

    /**
     * Ingestão de Notificação de Vídeo: Chamado pelo YouTube via POST quando sai vídeo novo.
     */
    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<ApiResponse<Void>> handleVideoNotification(
            @RequestHeader(name = "X-Hub-Signature", required = false) String signatureHeader,
            @RequestBody byte[] rawPayload
    ) {
        try {
            boolean success = webhookService.processNotification(rawPayload, signatureHeader);
            if (!success) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Assinatura HMAC inválida ou ausente"));
            }

            return ResponseEntity.ok(ApiResponse.ok("Evento processado e enfileirado com sucesso"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erro inesperado no processamento do webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erro interno ao processar notificação"));
        }
    }
}
