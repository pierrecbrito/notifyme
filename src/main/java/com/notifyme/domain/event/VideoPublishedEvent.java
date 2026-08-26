package com.notifyme.domain.event;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Domain Event: Published to 'notifyme.video.published' queue
 * once a YouTube WebSub webhook notification is successfully validated.
 * 
 * Uses Java 17 Record to provide:
 * 1. Complete immutability while in transit.
 * 2. Clean JSON serialization and deserialization.
 * 3. Compact representation with auto-generated equals, hashCode, and toString.
 */
public record VideoPublishedEvent(
        String videoId,
        String channelId,
        String title,
        String videoUrl,
        Instant publishedAt,
        Instant ingestedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Factory method that auto-populates the ingestion timestamp.
     */
    public static VideoPublishedEvent of(String videoId, String channelId, String title, String videoUrl, Instant publishedAt) {
        return new VideoPublishedEvent(
                videoId,
                channelId,
                title,
                videoUrl,
                publishedAt,
                Instant.now()
        );
    }
}
