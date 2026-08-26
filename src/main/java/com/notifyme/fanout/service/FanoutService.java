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
 * Service Layer for Notification Discovery and Fan-out Logic.
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
     * Executes the Fan-out process for a published video event:
     * 1. Checks and acquires an atomic idempotency lock in Redis (SETNX).
     * 2. Queries active subscribers from DynamoDB.
     * 3. Slices the subscriber list into chunks and enqueues individual delivery tasks to RabbitMQ.
     */
    public void processFanout(VideoPublishedEvent event) {
        long startTime = System.currentTimeMillis();

        // 1. Idempotency / Deduplication with Redis (SETNX)
        String lockKey = RedisKeyConstants.VIDEO_IDEMPOTENCY_PREFIX + event.videoId();
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSED", LOCK_EXPIRATION);

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.warn("Video '{}' ({}) has already been processed. Ignoring duplicate event.",
                    event.title(), event.videoId());
            return;
        }

        // 2. Query Partitioned Subscribers from DynamoDB
        List<String> subscriberUserIds = subscriptionRepository.findActiveSubscriberIdsByChannelId(event.channelId());
        int totalSubscribers = subscriberUserIds.size();

        if (totalSubscribers == 0) {
            log.info("No active subscribers found for channel '{}'", event.channelId());
            return;
        }

        log.info("Found {} active subscribers for channel '{}'. Slicing in batches of {}...",
                totalSubscribers, event.channelId(), chunkSize);

        // 3. Chunking and Enqueuing Delivery Tasks
        int dispatchedCount = 0;
        for (int i = 0; i < totalSubscribers; i += chunkSize) {
            int end = Math.min(i + chunkSize, totalSubscribers);
            List<String> chunk = subscriberUserIds.subList(i, end);

            for (String userId : chunk) {
                DeliveryTaskEvent task = DeliveryTaskEvent.from(userId, event);
                rabbitTemplate.convertAndSend(mainExchange, deliveryTaskRoutingKey, task);
                dispatchedCount++;
            }

            log.debug("Chunk [{}-{}] of {} tasks enqueued successfully", i + 1, end, totalSubscribers);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fan-out completed for video '{}'. {} tasks dispatched in {}ms",
                event.videoId(), dispatchedCount, duration);
    }
}
