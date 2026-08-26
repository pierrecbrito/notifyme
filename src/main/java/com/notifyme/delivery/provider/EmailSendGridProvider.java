package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email Notification Provider via SendGrid / AWS SES.
 */
@Slf4j
@Component
public class EmailSendGridProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(DeliveryTaskEvent task, UserPreference preference) {
        String email = preference.getEmail();

        if (email == null || email.isBlank()) {
            log.warn("[EMAIL SendGrid] User {} has no registered email address. Skipping dispatch.",
                    task.userId());
            return;
        }

        // In production, build SendGrid / SES API request with HTML templates
        log.info("[EMAIL SendGrid] ✉️ Sending email to <{}> (User {}). Subject: 'New video: {}' -> Link: {}",
                email, task.userId(), task.title(), task.videoUrl());
    }
}
