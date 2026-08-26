package com.notifyme.domain.event;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de Domínio: Publicado na fila 'notifyme.video.published'
 * assim que o Webhook do YouTube é validado com sucesso.
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
     * Construtor auxiliar para criar o evento preenchendo automaticamente o timestamp de ingestão.
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
