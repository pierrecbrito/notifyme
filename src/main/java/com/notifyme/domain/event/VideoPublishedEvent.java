package com.notifyme.domain.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de Domínio: Publicado na fila 'notifyme.video.published'
 * assim que o Webhook do YouTube é validado com sucesso.
 * 
 * Usamos Java 17 Record para garantir:
 * 1. Imutabilidade absoluta do evento em trânsito.
 * 2. Serialização e desserialização JSON instantânea e limpa.
 * 3. Criação de equals, hashCode e toString sem código repetitivo.
 */
public record VideoPublishedEvent(
        String videoId,
        String channelId,
        String title,
        String videoUrl,
        Instant publishedAt,
        Instant ingestedAt
) implements Serializable {

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
