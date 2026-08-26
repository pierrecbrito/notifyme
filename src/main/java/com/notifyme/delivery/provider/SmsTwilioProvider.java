package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS Notification Provider via Twilio.
 */
@Slf4j
@Component
public class SmsTwilioProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(DeliveryTaskEvent task, UserPreference preference) {
        String phoneNumber = preference.getPhoneNumber();

        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("[SMS Twilio] User {} has no registered phone number. Skipping dispatch.",
                    task.userId());
            return;
        }

        // In production, invoke Twilio Message.creator(...)
        String messageBody = String.format("NotifyMe: New video published! '%s' - Watch now: %s",
                task.title(), task.videoUrl());

        log.info("[SMS Twilio] 📱 Sending SMS to {} (User {}). Body: '{}'",
                phoneNumber, task.userId(), messageBody);
    }
}
