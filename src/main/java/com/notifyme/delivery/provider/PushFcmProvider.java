package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Push Notification Provider via Firebase Cloud Messaging (FCM).
 */
@Slf4j
@Component
public class PushFcmProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(DeliveryTaskEvent task, UserPreference preference) {
        List<String> deviceTokens = preference.getDeviceTokens();

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("[FCM PUSH] User {} has no registered device tokens. Skipping dispatch.",
                    task.userId());
            return;
        }

        // In production, invoke FirebaseMessaging.getInstance().sendEachForMulticast(message)
        log.info("[FCM PUSH] ✅ Dispatching Push to {} device(s) for user {}. Video: '{}' ({})",
                deviceTokens.size(), task.userId(), task.title(), task.videoUrl());
    }
}
