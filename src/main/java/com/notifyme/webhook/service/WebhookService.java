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
 * Camada de Serviço para Ingestão e Processamento de Webhooks do YouTube.
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
     * Valida a assinatura HMAC e processa a notificação XML do YouTube,
     * despachando os eventos de novos vídeos para o RabbitMQ.
     * 
     * @param rawPayload Conteúdo bruto da requisição HTTP (bytes).
     * @param signatureHeader Cabeçalho X-Hub-Signature com hash HMAC-SHA1.
     * @return true se processado e despachado com sucesso.
     */
    public boolean processNotification(byte[] rawPayload, String signatureHeader) {
        // 1. Validação de Segurança Criptográfica
        boolean isValid = signatureValidator.isValid(rawPayload, signatureHeader);
        if (!isValid) {
            log.warn("Rejeitando webhook: assinatura HMAC inválida ou ausente");
            return false;
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

                        // Parse de data seguro contra formatos inesperados
                        Instant publishedAt = parsePublishedDate(entry.getPublished());

                        // 3. Criação e Enfileiramento do Evento de Domínio
                        VideoPublishedEvent event = VideoPublishedEvent.of(
                                entry.getVideoId(),
                                entry.getChannelId(),
                                entry.getTitle(),
                                videoUrl,
                                publishedAt
                        );

                        rabbitTemplate.convertAndSend(mainExchange, videoPublishedRoutingKey, event);
                        log.info("Vídeo '{}' ({}) do canal '{}' despachado para a fila '{}'",
                                entry.getTitle(), entry.getVideoId(), entry.getChannelId(), videoPublishedRoutingKey);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Erro ao realizar o parse do payload XML do YouTube: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Formato XML inválido", e);
        }
    }

    /**
     * Parse robusto de data com fallback seguro para Instant.now() em caso de formatos atípicos.
     */
    private Instant parsePublishedDate(String publishedStr) {
        if (publishedStr == null || publishedStr.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(publishedStr);
        } catch (DateTimeParseException e) {
            log.warn("Data de publicação '{}' em formato não-ISO padrão. Usando Instant.now() como fallback.", publishedStr);
            return Instant.now();
        }
    }
}
