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
 * Service Layer for Multi-Channel Notification Execution and Delivery.
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
     * Processes an individual delivery task:
     * 1. Retrieves user preferences from Redis via UserPreferenceService.
     * 2. Dispatches to active channels with channel-isolated error handling to prevent duplicate retries.
     * 3. Audits end-to-end delivery latency.
     */
    public void processDelivery(DeliveryTaskEvent task) {
        long startTime = System.currentTimeMillis();

        // 1. Retrieve preferences from Redis Cache
        Optional<UserPreference> optPreference = userPreferenceService.getPreference(task.userId());
        if (optPreference.isEmpty()) {
            log.warn("User '{}' has no preferences stored in Redis. Skipping dispatch gracefully.", task.userId());
            return;
        }

        UserPreference preference = optPreference.get();
        Set<NotificationChannel> enabledChannels = preference.getEnabledChannels();

        if (enabledChannels == null || enabledChannels.isEmpty()) {
            log.info("User '{}' has no active notification channels.", task.userId());
            return;
        }

        // 2. Multi-Channel Dispatch with Per-Channel Error Isolation
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
                    log.error("Failed to deliver via {} to user '{}': {}", channel, task.userId(), e.getMessage());
                }
            } else {
                log.warn("No provider registered for channel {}", channel);
            }
        }

        // Throw exception to trigger RabbitMQ retry ONLY if ALL channels failed
        if (successCount == 0 && failureCount > 0) {
            throw new RuntimeException("Complete delivery failure for user " + task.userId() + ": " + String.join(", ", errors));
        }

        // 3. End-to-End Latency Auditing
        long processingTime = System.currentTimeMillis() - startTime;
        long e2eLatencyMs = task.ingestedAt() != null
                ? Duration.between(task.ingestedAt(), Instant.now()).toMillis()
                : processingTime;

        log.info("🎯 [DELIVERY COMPLETED] User: {} | Task: {} | Successes: {} | Failures: {} | Total Latency: {}ms",
                task.userId(), task.taskId(), successCount, failureCount, e2eLatencyMs);
    }
}
