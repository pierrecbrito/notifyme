package com.notifyme.webhook.controller;

import com.notifyme.api.dto.ApiResponse;
import com.notifyme.webhook.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * YouTube WebSub (PubSubHubbub) Webhook Ingestion Controller.
 * 
 * Thin HTTP entry layer that delegates processing to WebhookService.
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
     * WebSub Handshake: Called via GET by YouTube Hub upon subscribing to a channel topic.
     * Must return 200 OK with the 'hub.challenge' as plain text response body.
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleWebSubVerification(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.topic", required = false) String topic,
            @RequestParam(name = "hub.challenge", required = false) String challenge,
            @RequestParam(name = "hub.lease_seconds", required = false) String leaseSeconds
    ) {
        log.info("Received YouTube WebSub handshake. Mode: {}, Topic: {}", mode, topic);

        if (challenge == null || challenge.isBlank()) {
            log.warn("Handshake received without hub.challenge");
            return ResponseEntity.badRequest().body("Missing hub.challenge");
        }

        return ResponseEntity.ok(challenge);
    }

    /**
     * Video Publication Ingestion: Called via POST by YouTube when a new video is published.
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
                        .body(ApiResponse.error("Invalid or missing HMAC signature"));
            }

            return ResponseEntity.ok(ApiResponse.ok("Event processed and enqueued successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error processing webhook notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal error processing notification"));
        }
    }
}
