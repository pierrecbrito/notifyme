package com.notifyme.delivery.service;

import com.notifyme.api.service.UserPreferenceService;
import com.notifyme.delivery.provider.NotificationProvider;
import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Camada de Serviço para a Execução e Entrega Multicanal de Notificações.
 */
@Slf4j
@Service
public class DeliveryService {

    private final Map<NotificationChannel, NotificationProvider> providerMap;
    private final UserPreferenceService userPreferenceService;

    public DeliveryService(
            List<NotificationProvider> providers,
            UserPreferenceService userPreferenceService
    ) {
        this.userPreferenceService = userPreferenceService;
        this.providerMap = new EnumMap<>(NotificationChannel.class);

        for (NotificationProvider provider : providers) {
            this.providerMap.put(provider.getChannel(), provider);
        }
    }

    /**
     * Processa uma tarefa de entrega individual:
     * 1. Consulta preferências no Redis via UserPreferenceService.
     * 2. Despacha nos canais habilitados com isolamento de falhas para evitar re-envios duplicados.
     * 3. Audita a latência total da entrega.
     */
    public void processDelivery(DeliveryTaskEvent task) {
        long startTime = System.currentTimeMillis();

        // 1. Busca preferências no Redis Cache
        Optional<UserPreference> optPreference = userPreferenceService.getPreference(task.userId());
        if (optPreference.isEmpty()) {
            log.warn("Usuário '{}' não possui preferências cadastradas no Redis. Ignorando envio sem inventar dados falsos.", task.userId());
            return;
        }

        UserPreference preference = optPreference.get();
        Set<NotificationChannel> enabledChannels = preference.getEnabledChannels();

        if (enabledChannels == null || enabledChannels.isEmpty()) {
            log.info("Usuário '{}' não possui canais de notificação ativos.", task.userId());
            return;
        }

        // 2. Disparo Multicanal com Isolamento de Erros por Canal
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        for (NotificationChannel channel : enabledChannels) {
            NotificationProvider provider = providerMap.get(channel);
            if (provider != null) {
                try {
                    provider.send(task, preference);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errors.add(channel + ": " + e.getMessage());
                    log.error("Falha ao entregar via {} para usuário '{}': {}", channel, task.userId(), e.getMessage());
                }
            } else {
                log.warn("Nenhum provedor registrado para o canal {}", channel);
            }
        }

        // Se TODOS os canais falharem, lançamos exceção para o RabbitMQ reprocessar
        if (successCount == 0 && failureCount > 0) {
            throw new RuntimeException("Falha total na entrega para o usuário " + task.userId() + ": " + String.join(", ", errors));
        }

        // 3. Auditoria de Latência Ponta a Ponta
        long processingTime = System.currentTimeMillis() - startTime;
        long e2eLatencyMs = task.ingestedAt() != null
                ? Duration.between(task.ingestedAt(), Instant.now()).toMillis()
                : processingTime;

        log.info("🎯 [ENTREGA CONCLUÍDA] Usuário: {} | Tarefa: {} | Sucessos: {} | Falhas: {} | Latência Total: {}ms",
                task.userId(), task.taskId(), successCount, failureCount, e2eLatencyMs);
    }
}
