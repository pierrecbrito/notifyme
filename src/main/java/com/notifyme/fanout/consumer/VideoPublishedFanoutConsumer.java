package com.notifyme.fanout.consumer;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.fanout.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Consumidor de Fan-out: Ouve a fila 'notifyme.video.published'.
 * 
 * Responsabilidades:
 * 1. Garantir Idempotência (evitar disparar o mesmo vídeo mais de uma vez).
 * 2. Consultar os inscritos no DynamoDB.
 * 3. Fatiar a lista de inscritos e despachar as tarefas individuais na fila 'notifyme.delivery.tasks'.
 */
@Slf4j
@Service
public class VideoPublishedFanoutConsumer {

    private static final String VIDEO_IDEMPOTENCY_PREFIX = "lock:video:";
    private static final Duration LOCK_EXPIRATION = Duration.ofHours(24);

    private final SubscriptionRepository subscriptionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${notifyme.exchanges.main:notifyme.exchange}")
    private String mainExchange;

    @Value("${notifyme.routing-keys.delivery-task:delivery.task}")
    private String deliveryTaskRoutingKey;

    @Value("${notifyme.fanout.chunk-size:500}")
    private int chunkSize;

    public VideoPublishedFanoutConsumer(
            SubscriptionRepository subscriptionRepository,
            RedisTemplate<String, Object> redisTemplate,
            RabbitTemplate rabbitTemplate
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "${notifyme.queues.video-published:notifyme.video.published}")
    public void onVideoPublished(VideoPublishedEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("Iniciando Fan-out para o vídeo '{}' ({}) do canal '{}'",
                event.title(), event.videoId(), event.channelId());

        // 1. Idempotência / Deduplicação com Redis (SETNX com TTL de 24h)
        String lockKey = VIDEO_IDEMPOTENCY_PREFIX + event.videoId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSED", LOCK_EXPIRATION);

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.warn("Vídeo '{}' ({}) já foi processado anteriormente. Ignorando evento duplicado.",
                    event.title(), event.videoId());
            return;
        }

        // 2. Consulta de Inscritos Particionados no DynamoDB
        List<String> subscriberUserIds = subscriptionRepository.findActiveSubscriberIdsByChannelId(event.channelId());
        int totalSubscribers = subscriberUserIds.size();

        if (totalSubscribers == 0) {
            log.info("Nenhum seguidor ativo encontrado para o canal '{}'", event.channelId());
            return;
        }

        log.info("Encontrados {} seguidores ativos. Fatiando e despachando tarefas de entrega...", totalSubscribers);

        // 3. Fatiamento em Lotes (Chunks) e Enfileiramento das Tarefas de Entrega
        int dispatchedCount = 0;
        for (int i = 0; i < totalSubscribers; i += chunkSize) {
            int end = Math.min(i + chunkSize, totalSubscribers);
            List<String> chunk = subscriberUserIds.subList(i, end);

            for (String userId : chunk) {
                DeliveryTaskEvent task = DeliveryTaskEvent.from(userId, event);
                rabbitTemplate.convertAndSend(mainExchange, deliveryTaskRoutingKey, task);
                dispatchedCount++;
            }

            log.debug("Chunk [{}-{}] de {} tarefas enfileirado com sucesso", i + 1, end, totalSubscribers);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fan-out concluído para o vídeo '{}'. {} tarefas despachadas em {}ms",
                event.videoId(), dispatchedCount, duration);
    }
}
