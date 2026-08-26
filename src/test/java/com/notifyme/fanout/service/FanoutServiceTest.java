package com.notifyme.fanout.service;

import com.notifyme.config.constants.RedisKeyConstants;
import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.fanout.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanoutServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private FanoutService fanoutService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        fanoutService = new FanoutService(
                subscriptionRepository,
                redisTemplate,
                rabbitTemplate
        );

        ReflectionTestUtils.setField(fanoutService, "mainExchange", "notifyme.exchange");
        ReflectionTestUtils.setField(fanoutService, "deliveryTaskRoutingKey", "delivery.task");
        ReflectionTestUtils.setField(fanoutService, "chunkSize", 2); // chunk pequeno para teste
    }

    @Test
    @DisplayName("Deve executar o fan-out e fatiar seguidores em chunks para envio ao RabbitMQ")
    void shouldProcessFanoutInChunks() {
        VideoPublishedEvent event = VideoPublishedEvent.of(
                "v123", "UC_CHANNEL", "Vídeo de Teste", "https://youtube.com/v123", Instant.now()
        );

        String lockKey = RedisKeyConstants.VIDEO_IDEMPOTENCY_PREFIX + "v123";
        when(valueOperations.setIfAbsent(eq(lockKey), eq("PROCESSED"), any(Duration.class))).thenReturn(true);

        when(subscriptionRepository.findActiveSubscriberIdsByChannelId("UC_CHANNEL"))
                .thenReturn(List.of("user_1", "user_2", "user_3"));

        fanoutService.processFanout(event);

        // 3 seguidores com chunkSize = 2 deve despachar 3 tarefas
        verify(rabbitTemplate, times(3)).convertAndSend(eq("notifyme.exchange"), eq("delivery.task"), any(Object.class));
    }

    @Test
    @DisplayName("Deve descartar evento duplicado se a trava de idempotência já existir no Redis")
    void shouldSkipDuplicateVideoEvent() {
        VideoPublishedEvent event = VideoPublishedEvent.of(
                "v123", "UC_CHANNEL", "Vídeo Duplicado", "https://youtube.com/v123", Instant.now()
        );

        String lockKey = RedisKeyConstants.VIDEO_IDEMPOTENCY_PREFIX + "v123";
        // Já existe no Redis (setIfAbsent retorna false)
        when(valueOperations.setIfAbsent(eq(lockKey), eq("PROCESSED"), any(Duration.class))).thenReturn(false);

        fanoutService.processFanout(event);

        // Não deve consultar DynamoDB nem publicar no RabbitMQ
        verifyNoInteractions(subscriptionRepository);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
