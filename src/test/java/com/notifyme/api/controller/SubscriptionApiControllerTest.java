package com.notifyme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.api.dto.SubscribeRequestDto;
import com.notifyme.api.service.SubscriptionService;
import com.notifyme.api.service.UserPreferenceService;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionApiController.class)
class SubscriptionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @Test
    @DisplayName("POST /api/v1/subscriptions - Should subscribe user with status 201 Created")
    void shouldSubscribeUserSuccessfully() throws Exception {
        SubscribeRequestDto request = SubscribeRequestDto.builder()
                .channelId("UC123")
                .userId("user_joao")
                .build();

        doNothing().when(subscriptionService).subscribe(any(SubscribeRequestDto.class));

        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User 'user_joao' subscribed successfully to channel 'UC123'"));

        verify(subscriptionService, times(1)).subscribe(any(SubscribeRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/v1/subscriptions/{channelId} - Should return subscriber list with status 200 OK")
    void shouldGetChannelSubscribers() throws Exception {
        when(subscriptionService.getActiveSubscribers("UC123")).thenReturn(List.of("user_joao", "user_maria"));

        mockMvc.perform(get("/api/v1/subscriptions/UC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").value("user_joao"))
                .andExpect(jsonPath("$.data[1]").value("user_maria"));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/preferences - Should save user preferences in Redis with status 200 OK")
    void shouldSaveUserPreferences() throws Exception {
        UserPreference preference = UserPreference.builder()
                .enabledChannels(Set.of(NotificationChannel.PUSH, NotificationChannel.EMAIL))
                .email("joao@example.com")
                .deviceTokens(List.of("fcm_token_123"))
                .build();

        mockMvc.perform(post("/api/v1/users/user_joao/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Preferences saved successfully in Redis for user: user_joao"));

        verify(userPreferenceService, times(1)).savePreference(any(UserPreference.class));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/preferences - Should return cached user preferences")
    void shouldGetUserPreferences() throws Exception {
        UserPreference preference = UserPreference.builder()
                .userId("user_joao")
                .enabledChannels(Set.of(NotificationChannel.PUSH))
                .email("joao@example.com")
                .build();

        when(userPreferenceService.getPreference("user_joao")).thenReturn(Optional.of(preference));

        mockMvc.perform(get("/api/v1/users/user_joao/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user_joao"))
                .andExpect(jsonPath("$.data.email").value("joao@example.com"));
    }
}
