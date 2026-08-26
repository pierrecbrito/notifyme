package com.notifyme.api.service;

import com.notifyme.api.dto.SubscribeRequestDto;
import com.notifyme.domain.model.UserSubscription;
import com.notifyme.fanout.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service Layer for User Channel Subscriptions in DynamoDB.
 */
@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Subscribes a user to a YouTube creator channel.
     */
    public void subscribe(SubscribeRequestDto request) {
        if (request.getChannelId() == null || request.getChannelId().isBlank() ||
                request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("channelId and userId are required");
        }

        UserSubscription subscription = UserSubscription.builder()
                .channelId(request.getChannelId().trim())
                .userId(request.getUserId().trim())
                .active(true)
                .createdAt(Instant.now())
                .build();

        subscriptionRepository.save(subscription);
        log.info("Subscription created: user '{}' -> channel '{}'", request.getUserId(), request.getChannelId());
    }

    /**
     * Retrieves active subscriber user IDs for a given channel.
     */
    public List<String> getActiveSubscribers(String channelId) {
        return subscriptionRepository.findActiveSubscriberIdsByChannelId(channelId);
    }
}
