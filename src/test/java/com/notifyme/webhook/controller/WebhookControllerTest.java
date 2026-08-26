package com.notifyme.webhook.controller;

import com.notifyme.webhook.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookService webhookService;

    @Test
    @DisplayName("GET /api/v1/webhook/youtube - Deve responder ao handshake WebSub com o hub.challenge")
    void shouldRespondToWebSubHandshake() throws Exception {
        mockMvc.perform(get("/api/v1/webhook/youtube")
                        .param("hub.mode", "subscribe")
                        .param("hub.topic", "https://www.youtube.com/xml/feeds/videos.xml?channel_id=UC123")
                        .param("hub.challenge", "challenge_token_abc_987")
                        .param("hub.lease_seconds", "864000"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("challenge_token_abc_987"));
    }

    @Test
    @DisplayName("GET /api/v1/webhook/youtube - Deve retornar 400 se hub.challenge não for informado")
    void shouldReturnBadRequestWhenChallengeMissing() throws Exception {
        mockMvc.perform(get("/api/v1/webhook/youtube"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("hub.challenge ausente"));
    }

    @Test
    @DisplayName("POST /api/v1/webhook/youtube - Deve retornar 200 OK quando o webhook for válido")
    void shouldAcceptValidWebhookNotification() throws Exception {
        String xmlPayload = "<feed><entry><yt:videoId>v123</yt:videoId></entry></feed>";
        String signature = "sha1=valid_hash_123";

        when(webhookService.processNotification(any(byte[].class), eq(signature))).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhook/youtube")
                        .contentType(MediaType.APPLICATION_ATOM_XML_VALUE)
                        .header("X-Hub-Signature", signature)
                        .content(xmlPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Evento processado e enfileirado com sucesso"));
    }

    @Test
    @DisplayName("POST /api/v1/webhook/youtube - Deve retornar 403 Forbidden se a assinatura HMAC for inválida")
    void shouldRejectInvalidHmacSignature() throws Exception {
        String xmlPayload = "<feed><entry><yt:videoId>fake_video</yt:videoId></entry></feed>";
        String signature = "sha1=invalid_signature";

        when(webhookService.processNotification(any(byte[].class), eq(signature))).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhook/youtube")
                        .contentType(MediaType.APPLICATION_ATOM_XML_VALUE)
                        .header("X-Hub-Signature", signature)
                        .content(xmlPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assinatura HMAC inválida ou ausente"));
    }
}
