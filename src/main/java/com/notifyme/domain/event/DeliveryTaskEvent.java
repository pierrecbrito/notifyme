package com.notifyme.domain.event;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Delivery Task Event: Created by the Fan-out Service and published to 'notifyme.delivery.tasks'.
 * 
 * Each instance represents an individual work unit for dispatching
 * a new video notification to a specific channel subscriber.
 */
public record DeliveryTaskEvent(
        String taskId,
        String userId,
        String channelId,
        String videoId,
        String title,
        String videoUrl,
        Instant publishedAt,
        Instant ingestedAt,
        int attempt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Factory constructor that generates an automatic taskId and initial attempt count (attempt = 1).
     */
    public static DeliveryTaskEvent from(
            String userId,
            VideoPublishedEvent videoEvent
    ) {
        return new DeliveryTaskEvent(
                UUID.randomUUID().toString(),
                userId,
                videoEvent.channelId(),
                videoEvent.videoId(),
                videoEvent.title(),
                videoEvent.videoUrl(),
                videoEvent.publishedAt(),
                videoEvent.ingestedAt(),
                1
        );
    }
}
