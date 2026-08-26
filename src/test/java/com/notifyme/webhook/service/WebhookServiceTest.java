package com.notifyme.webhook.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.webhook.security.HmacSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private HmacSignatureValidator signatureValidator;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(
                signatureValidator,
                rabbitTemplate,
                new XmlMapper()
        );

        ReflectionTestUtils.setField(webhookService, "mainExchange", "notifyme.exchange");
        ReflectionTestUtils.setField(webhookService, "videoPublishedRoutingKey", "video.published");
    }

    @Test
    @DisplayName("Deve validar assinatura, processar XML Atom do YouTube e publicar VideoPublishedEvent no RabbitMQ")
    void shouldProcessNotificationAndPublishEvent() {
        String xml = """
                <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns="http://www.w3.org/2005/Atom">
                  <entry>
                    <yt:videoId>v_987xyz</yt:videoId>
                    <yt:channelId>UC_CANAL_01</yt:channelId>
                    <title>Vídeo Novo Incrível</title>
                    <link rel="alternate" href="https://www.youtube.com/watch?v=v_987xyz"/>
                    <published>2026-08-26T10:00:00+00:00</published>
                  </entry>
                </feed>
                """;

        byte[] rawPayload = xml.getBytes(StandardCharsets.UTF_8);
        String signature = "sha1=fake_valid_signature";

        when(signatureValidator.isValid(rawPayload, signature)).thenReturn(true);

        boolean result = webhookService.processNotification(rawPayload, signature);

        assertTrue(result, "Deveria retornar true após processar com sucesso");

        ArgumentCaptor<VideoPublishedEvent> eventCaptor = ArgumentCaptor.forClass(VideoPublishedEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("notifyme.exchange"), eq("video.published"), eventCaptor.capture());

        VideoPublishedEvent captured = eventCaptor.getValue();
        assertEquals("v_987xyz", captured.videoId());
        assertEquals("UC_CANAL_01", captured.channelId());
        assertEquals("Vídeo Novo Incrível", captured.title());
        assertEquals("https://www.youtube.com/watch?v=v_987xyz", captured.videoUrl());
    }

    @Test
    @DisplayName("Deve rejeitar processamento se a assinatura HMAC for inválida")
    void shouldRejectWhenSignatureIsInvalid() {
        byte[] rawPayload = "<feed></feed>".getBytes(StandardCharsets.UTF_8);
        String signature = "sha1=wrong";

        when(signatureValidator.isValid(rawPayload, signature)).thenReturn(false);

        boolean result = webhookService.processNotification(rawPayload, signature);

        assertFalse(result, "Deveria retornar false quando a assinatura for inválida");
        verifyNoInteractions(rabbitTemplate);
    }
}
