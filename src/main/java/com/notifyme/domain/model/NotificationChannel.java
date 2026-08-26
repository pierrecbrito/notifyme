package com.notifyme.domain.model;

/**
 * Enum que define os canais de notificação suportados pelo NotifyMe.
 */
public enum NotificationChannel {
    /**
     * Notificação Push enviada para dispositivos móveis via Firebase Cloud Messaging (FCM).
     */
    PUSH,

    /**
     * E-mail transacional enviado via SendGrid / AWS SES.
     */
    EMAIL,

    /**
     * Mensagem SMS enviada via Twilio.
     */
    SMS
}
