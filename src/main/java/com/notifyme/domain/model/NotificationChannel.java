package com.notifyme.domain.model;

/**
 * Enum defining the supported notification delivery channels in NotifyMe.
 */
public enum NotificationChannel {
    /**
     * Mobile push notification sent via Firebase Cloud Messaging (FCM).
     */
    PUSH,

    /**
     * Transactional email sent via SendGrid / AWS SES.
     */
    EMAIL,

    /**
     * SMS text message sent via Twilio.
     */
    SMS
}
