package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;

/**
 * Strategy Contract (Interface) for Notification Providers.
 * 
 * Implements the Strategy design pattern: each delivery channel provider
 * (FCM, SendGrid, Twilio) implements this interface and is dynamically triggered
 * based on user active channel preferences.
 */
public interface NotificationProvider {

    /**
     * Returns the channel supported by this provider (PUSH, EMAIL, SMS).
     */
    NotificationChannel getChannel();

    /**
     * Dispatches the notification to the target user.
     * 
     * @param task Notification metadata for the published video.
     * @param preference User contact details and active preferences (from Redis in-memory cache).
     */
    void send(DeliveryTaskEvent task, UserPreference preference);
}
