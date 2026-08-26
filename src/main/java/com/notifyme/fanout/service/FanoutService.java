package com.notifyme.fanout.service;

import com.notifyme.config.constants.RedisKeyConstants;
import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.fanout.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Camada de Serviço para a Lógica de Descoberta e Fan-out de Notificações.
 */
@Slf4j
@Service
public class FanoutService {

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

    public FanoutService(
            SubscriptionRepository subscriptionRepository,
            RedisTemplate<String, Object> redisTemplate,
            RabbitTemplate rabbitTemplate
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Executa o processo de Fan-out para um evento de vídeo publicado:
     * 1. Verifica e adquire trava de idempotência no Redis.
     * 2. Consulta seguidores ativos no DynamoDB.
     * 3. Fatia a lista em chunks e enfileira as tarefas individuais no RabbitMQ.
     */
    public void processFanout(VideoPublishedEvent event) {
        long startTime = System.currentTimeMillis();

        // 1. Idempotência / Deduplicação com Redis (SETNX)
        String lockKey = RedisKeyConstants.VIDEO_IDEMPOTENCY_PREFIX + event.videoId();
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

        log.info("Encontrados {} seguidores ativos para o canal '{}'. Fatiando em lotes de {}...",
                totalSubscribers, event.channelId(), chunkSize);

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
