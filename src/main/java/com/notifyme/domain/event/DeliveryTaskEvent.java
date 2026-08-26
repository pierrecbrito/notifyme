package com.notifyme.domain.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Tarefa de Envio: Gerada pelo Fan-out Service e enfileirada em 'notifyme.delivery.tasks'.
 * 
 * Cada instância desta mensagem representa o trabalho individual de entregar
 * a notificação de um novo vídeo para um usuário específico (seguidor).
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

    /**
     * Construtor de fábrica para criar uma nova tarefa com taskId gerado automaticamente
     * e primeira tentativa (attempt = 1).
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
