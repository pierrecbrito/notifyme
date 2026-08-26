package com.notifyme.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User Preferences Model (Cached in Redis).
 * 
 * Contains selected notification channels and user contact details
 * required for immediate dispatch without querying relational databases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;

    /**
     * Channels enabled by the user (e.g., PUSH, EMAIL, SMS).
     */
    @Builder.Default
    private Set<NotificationChannel> enabledChannels = Set.of(NotificationChannel.PUSH);

    /**
     * Destination email for SendGrid / AWS SES.
     */
    private String email;

    /**
     * Destination phone number in E.164 format (e.g., +15551234567) for Twilio SMS.
     */
    private String phoneNumber;

    /**
     * Mobile device registration tokens for Firebase Cloud Messaging (FCM).
     */
    @Builder.Default
    private List<String> deviceTokens = Collections.emptyList();

    /**
     * Checks if a given notification channel is active for this user.
     */
    public boolean isChannelEnabled(NotificationChannel channel) {
        return enabledChannels != null && enabledChannels.contains(channel);
    }
}
