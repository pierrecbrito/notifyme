package com.notifyme.delivery.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.delivery.provider.NotificationProvider;
import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Worker de Entrega: Ouve a fila 'notifyme.delivery.tasks'.
 * 
 * Responsabilidades:
 * 1. Consultar preferências do usuário no Redis Cache em sub-milissegundos (< 1ms).
 * 2. Rotear para os provedores configurados (FCM, SendGrid, Twilio).
 * 3. Auditar a latência ponta-a-ponta (< 5s desde a chegada do Webhook).
 */
@Slf4j
@Service
public class DeliveryTaskConsumer {

    private static final String USER_PREFS_KEY_PREFIX = "user:prefs:";

    private final Map<NotificationChannel, NotificationProvider> providerMap;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryTaskConsumer(
            List<NotificationProvider> providers,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.providerMap = new EnumMap<>(NotificationChannel.class);

        // Mapeia automaticamente cada provedor para o seu respectivo canal
        for (NotificationProvider provider : providers) {
            this.providerMap.put(provider.getChannel(), provider);
        }
    }

    @RabbitListener(queues = "${notifyme.queues.delivery-tasks:notifyme.delivery.tasks}")
    public void onDeliveryTask(DeliveryTaskEvent task) {
        long startTime = System.currentTimeMillis();

        // 1. Busca preferências no Redis Cache
        String cacheKey = USER_PREFS_KEY_PREFIX + task.userId();
        Object rawCachedPrefs = redisTemplate.opsForValue().get(cacheKey);

        UserPreference preference = resolveUserPreference(task.userId(), rawCachedPrefs);

        // 2. Disparo Multicanal baseado nas preferências ativas
        Set<NotificationChannel> enabledChannels = preference.getEnabledChannels();
        if (enabledChannels == null || enabledChannels.isEmpty()) {
            log.info("Usuário {} não possui nenhum canal de notificação ativo. Tarefa concluída.", task.userId());
            return;
        }

        for (NotificationChannel channel : enabledChannels) {
            NotificationProvider provider = providerMap.get(channel);
            if (provider != null) {
                try {
                    provider.send(task, preference);
                } catch (Exception e) {
                    log.error("Erro ao enviar notificação via {} para o usuário {}: {}",
                            channel, task.userId(), e.getMessage(), e);
                    // O Spring RabbitMQ gerencia o retry automático com Exponential Backoff e envio à DLQ
                    throw e;
                }
            } else {
                log.warn("Nenhum provedor configurado para o canal {}", channel);
            }
        }

        // 3. Auditoria de Latência Ponta a Ponta
        long processingTime = System.currentTimeMillis() - startTime;
        long e2eLatencyMs = task.ingestedAt() != null
                ? Duration.between(task.ingestedAt(), Instant.now()).toMillis()
                : processingTime;

        log.info("🎯 [ENTREGA CONCLUÍDA] Usuário: {} | Tarefa: {} | Processamento Worker: {}ms | Latência Total Ponta a Ponta: {}ms",
                task.userId(), task.taskId(), processingTime, e2eLatencyMs);
    }

    /**
     * Recupera o UserPreference do cache ou gera uma configuração padrão caso o usuário
     * ainda não tenha preenchido o perfil (garante alta disponibilidade sem travar o worker).
     */
    private UserPreference resolveUserPreference(String userId, Object rawCached) {
        if (rawCached != null) {
            try {
                if (rawCached instanceof UserPreference up) {
                    return up;
                }
                return objectMapper.convertValue(rawCached, UserPreference.class);
            } catch (Exception e) {
                log.warn("Erro ao desserializar preferências do Redis para o usuário {}. Usando fallback.", userId);
            }
        }

        // Fallback padrão: notificação Push com token mockado
        return UserPreference.builder()
                .userId(userId)
                .enabledChannels(Set.of(NotificationChannel.PUSH))
                .deviceTokens(List.of("fcm_token_device_" + userId))
                .build();
    }
}
