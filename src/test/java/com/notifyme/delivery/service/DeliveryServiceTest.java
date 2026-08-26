package com.notifyme.delivery.service;

import com.notifyme.api.service.UserPreferenceService;
import com.notifyme.delivery.provider.NotificationProvider;
import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private NotificationProvider pushProvider;

    @Mock
    private NotificationProvider emailProvider;

    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        lenient().when(pushProvider.getChannel()).thenReturn(NotificationChannel.PUSH);
        lenient().when(emailProvider.getChannel()).thenReturn(NotificationChannel.EMAIL);

        deliveryService = new DeliveryService(
                List.of(pushProvider, emailProvider),
                userPreferenceService
        );
    }

    @Test
    @DisplayName("Should deliver notifications only to channels enabled by the user")
    void shouldDeliverToEnabledChannels() {
        DeliveryTaskEvent task = new DeliveryTaskEvent(
                "task-1", "user-1", "channel-1", "video-1",
                "New Video", "https://youtube.com/v1", Instant.now(), Instant.now(), 1
        );

        UserPreference preference = UserPreference.builder()
                .userId("user-1")
                .enabledChannels(Set.of(NotificationChannel.PUSH))
                .deviceTokens(List.of("token-123"))
                .build();

        when(userPreferenceService.getPreference("user-1")).thenReturn(Optional.of(preference));

        deliveryService.processDelivery(task);

        verify(pushProvider, times(1)).send(task, preference);
        verify(emailProvider, never()).send(any(), any());
    }

    @Test
    @DisplayName("Should gracefully skip without error or fake tokens when user has no cached preferences")
    void shouldGracefullySkipWhenNoPreferences() {
        DeliveryTaskEvent task = new DeliveryTaskEvent(
                "task-2", "user-unknown", "channel-1", "video-1",
                "New Video", "https://youtube.com/v1", Instant.now(), Instant.now(), 1
        );

        when(userPreferenceService.getPreference("user-unknown")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> deliveryService.processDelivery(task));
        verify(pushProvider, never()).send(any(), any());
        verify(emailProvider, never()).send(any(), any());
    }

    @Test
    @DisplayName("Should throw exception to trigger RabbitMQ retry ONLY if ALL channels fail")
    void shouldThrowExceptionWhenAllChannelsFail() {
        DeliveryTaskEvent task = new DeliveryTaskEvent(
                "task-3", "user-3", "channel-1", "video-1",
                "New Video", "https://youtube.com/v1", Instant.now(), Instant.now(), 1
        );

        UserPreference preference = UserPreference.builder()
                .userId("user-3")
                .enabledChannels(Set.of(NotificationChannel.PUSH))
                .deviceTokens(List.of("token-fail"))
                .build();

        when(userPreferenceService.getPreference("user-3")).thenReturn(Optional.of(preference));
        doThrow(new RuntimeException("FCM connection timeout")).when(pushProvider).send(any(), any());

        assertThrows(RuntimeException.class, () -> deliveryService.processDelivery(task));
    }
}
