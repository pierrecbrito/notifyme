package com.notifyme.api.service;

import com.notifyme.api.dto.SubscribeRequestDto;
import com.notifyme.domain.model.UserSubscription;
import com.notifyme.fanout.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Camada de Serviço para Inscrições de Usuários no DynamoDB.
 */
@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Inscreve um usuário em um canal do YouTube.
     */
    public void subscribe(SubscribeRequestDto request) {
        if (request.getChannelId() == null || request.getChannelId().isBlank() ||
                request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("channelId e userId são obrigatórios");
        }

        UserSubscription subscription = UserSubscription.builder()
                .channelId(request.getChannelId().trim())
                .userId(request.getUserId().trim())
                .active(true)
                .createdAt(Instant.now())
                .build();

        subscriptionRepository.save(subscription);
        log.info("Inscrição concluída: usuário '{}' -> canal '{}'", request.getUserId(), request.getChannelId());
    }

    /**
     * Retorna os IDs dos inscritos ativos de um canal.
     */
    public List<String> getActiveSubscribers(String channelId) {
        return subscriptionRepository.findActiveSubscriberIdsByChannelId(channelId);
    }
}
