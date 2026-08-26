package com.notifyme.api.controller;

import com.notifyme.api.dto.ApiResponse;
import com.notifyme.api.dto.SubscribeRequestDto;
import com.notifyme.api.service.SubscriptionService;
import com.notifyme.api.service.UserPreferenceService;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Managing Subscriptions and User Preferences.
 * 
 * REST API layer exposing standardized ApiResponse<T> envelopes and delegating to services.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class SubscriptionApiController {

    private final SubscriptionService subscriptionService;
    private final UserPreferenceService userPreferenceService;

    public SubscriptionApiController(
            SubscriptionService subscriptionService,
            UserPreferenceService userPreferenceService
    ) {
        this.subscriptionService = subscriptionService;
        this.userPreferenceService = userPreferenceService;
    }

    /**
     * Subscribes a user to a YouTube creator channel (stored in DynamoDB via SubscriptionService).
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<ApiResponse<Void>> subscribeUser(@RequestBody SubscribeRequestDto request) {
        try {
            subscriptionService.subscribe(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(String.format("User '%s' subscribed successfully to channel '%s'",
                            request.getUserId(), request.getChannelId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Returns the list of active subscribers for a given YouTube creator channel.
     */
    @GetMapping("/subscriptions/{channelId}")
    public ResponseEntity<ApiResponse<List<String>>> getSubscribers(@PathVariable String channelId) {
        List<String> subscribers = subscriptionService.getActiveSubscribers(channelId);
        return ResponseEntity.ok(ApiResponse.ok("Subscribers retrieved successfully", subscribers));
    }

    /**
     * Creates or updates notification preferences for a user (stored in Redis Cache).
     */
    @PostMapping("/users/{userId}/preferences")
    public ResponseEntity<ApiResponse<Void>> saveUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreference preference
    ) {
        preference.setUserId(userId);
        userPreferenceService.savePreference(preference);

        return ResponseEntity.ok(ApiResponse.ok("Preferences saved successfully in Redis for user: " + userId));
    }

    /**
     * Retrieves cached user notification preferences from Redis.
     */
    @GetMapping("/users/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserPreference>> getUserPreferences(@PathVariable String userId) {
        Optional<UserPreference> preference = userPreferenceService.getPreference(userId);

        if (preference.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No cached preferences found for user: " + userId));
        }

        return ResponseEntity.ok(ApiResponse.ok("Preferences found", preference.get()));
    }
}
