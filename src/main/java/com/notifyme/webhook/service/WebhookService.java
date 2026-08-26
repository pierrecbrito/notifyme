package com.notifyme.webhook.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.webhook.dto.WebSubXmlFeedDto;
import com.notifyme.webhook.security.HmacSignatureValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Service Layer for YouTube Webhook Ingestion and Processing.
 */
@Slf4j
@Service
public class WebhookService {

    private final HmacSignatureValidator signatureValidator;
    private final RabbitTemplate rabbitTemplate;
    private final XmlMapper xmlMapper;

    @Value("${notifyme.exchanges.main:notifyme.exchange}")
    private String mainExchange;

    @Value("${notifyme.routing-keys.video-published:video.published}")
    private String videoPublishedRoutingKey;

    public WebhookService(
            HmacSignatureValidator signatureValidator,
            RabbitTemplate rabbitTemplate,
            XmlMapper xmlMapper
    ) {
        this.signatureValidator = signatureValidator;
        this.rabbitTemplate = rabbitTemplate;
        this.xmlMapper = xmlMapper;
    }

    /**
     * Validates HMAC signature and processes the YouTube XML notification,
     * publishing new video events to RabbitMQ.
     * 
     * @param rawPayload Raw HTTP request body bytes.
     * @param signatureHeader X-Hub-Signature header with HMAC-SHA1 hash.
     * @return true if successfully processed and dispatched.
     */
    public boolean processNotification(byte[] rawPayload, String signatureHeader) {
        // 1. Cryptographic Security Validation
        boolean isValid = signatureValidator.isValid(rawPayload, signatureHeader);
        if (!isValid) {
            log.warn("Rejecting webhook: invalid or missing HMAC signature");
            return false;
        }

        // 2. Parse XML Atom Feed
        try {
            WebSubXmlFeedDto feed = xmlMapper.readValue(rawPayload, WebSubXmlFeedDto.class);

            if (feed.getEntries() != null && !feed.getEntries().isEmpty()) {
                for (WebSubXmlFeedDto.FeedEntry entry : feed.getEntries()) {
                    if (entry.getVideoId() != null && entry.getChannelId() != null) {
                        String videoUrl = (entry.getLink() != null && entry.getLink().getHref() != null)
                                ? entry.getLink().getHref()
                                : "https://www.youtube.com/watch?v=" + entry.getVideoId();

                        // Safe date parsing with fallback
                        Instant publishedAt = parsePublishedDate(entry.getPublished());

                        // 3. Domain Event Creation and Enqueuing
                        VideoPublishedEvent event = VideoPublishedEvent.of(
                                entry.getVideoId(),
                                entry.getChannelId(),
                                entry.getTitle(),
                                videoUrl,
                                publishedAt
                        );

                        rabbitTemplate.convertAndSend(mainExchange, videoPublishedRoutingKey, event);
                        log.info("Video '{}' ({}) from channel '{}' dispatched to queue '{}'",
                                entry.getTitle(), entry.getVideoId(), entry.getChannelId(), videoPublishedRoutingKey);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to parse YouTube XML payload: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid XML format", e);
        }
    }

    /**
     * Resilient date parsing with fallback to Instant.now() for atypical formats.
     */
    private Instant parsePublishedDate(String publishedStr) {
        if (publishedStr == null || publishedStr.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(publishedStr);
        } catch (DateTimeParseException e) {
            log.warn("Publication date '{}' is not in standard ISO format. Using Instant.now() as fallback.", publishedStr);
            return Instant.now();
        }
    }
}
