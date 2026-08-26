package com.notifyme.webhook.controller;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.webhook.dto.WebSubXmlFeedDto;
import com.notifyme.webhook.security.HmacSignatureValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Controller de Ingestão de Webhooks do YouTube WebSub (PubSubHubbub).
 * 
 * Responsabilidades:
 * 1. GET: Handshake de verificação do YouTube (retorna hub.challenge).
 * 2. POST: Ingestão de novos vídeos em menos de 100ms com validação criptográfica HMAC-SHA1.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook/youtube")
public class WebhookController {

    private final HmacSignatureValidator signatureValidator;
    private final RabbitTemplate rabbitTemplate;
    private final XmlMapper xmlMapper;

    @Value("${notifyme.exchanges.main:notifyme.exchange}")
    private String mainExchange;

    @Value("${notifyme.routing-keys.video-published:video.published}")
    private String videoPublishedRoutingKey;

    public WebhookController(
            HmacSignatureValidator signatureValidator,
            RabbitTemplate rabbitTemplate
    ) {
        this.signatureValidator = signatureValidator;
        this.rabbitTemplate = rabbitTemplate;
        this.xmlMapper = new XmlMapper();
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
    public ResponseEntity<String> handleVideoNotification(
            @RequestHeader(name = "X-Hub-Signature", required = false) String signatureHeader,
            @RequestBody byte[] rawPayload
    ) {
        long startTime = System.currentTimeMillis();

        // 1. Validação de Segurança Criptográfica
        boolean isValid = signatureValidator.isValid(rawPayload, signatureHeader);
        if (!isValid) {
            log.warn("Rejeitando webhook: assinatura HMAC inválida ou ausente");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Assinatura inválida");
        }

        // 2. Parsing do XML Atom Feed
        try {
            WebSubXmlFeedDto feed = xmlMapper.readValue(rawPayload, WebSubXmlFeedDto.class);

            if (feed.getEntries() != null && !feed.getEntries().isEmpty()) {
                for (WebSubXmlFeedDto.FeedEntry entry : feed.getEntries()) {
                    if (entry.getVideoId() != null && entry.getChannelId() != null) {
                        String videoUrl = (entry.getLink() != null && entry.getLink().getHref() != null)
                                ? entry.getLink().getHref()
                                : "https://www.youtube.com/watch?v=" + entry.getVideoId();

                        Instant publishedAt = (entry.getPublished() != null)
                                ? Instant.parse(entry.getPublished())
                                : Instant.now();

                        // 3. Criação do Evento de Domínio
                        VideoPublishedEvent event = VideoPublishedEvent.of(
                                entry.getVideoId(),
                                entry.getChannelId(),
                                entry.getTitle(),
                                videoUrl,
                                publishedAt
                        );

                        // 4. Despacho não-bloqueante para a fila do RabbitMQ
                        rabbitTemplate.convertAndSend(mainExchange, videoPublishedRoutingKey, event);
                        log.info("Vídeo {} do canal {} despachado para a fila '{}'",
                                entry.getVideoId(), entry.getChannelId(), videoPublishedRoutingKey);
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Webhook processado com sucesso em {}ms", duration);

            // Responde 200 OK imediatamente para o YouTube
            return ResponseEntity.ok("Processado e enfileirado com sucesso");

        } catch (Exception e) {
            log.error("Erro ao realizar o parse do payload XML do YouTube: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro no formato XML");
        }
    }
}
