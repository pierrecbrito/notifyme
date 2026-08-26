package com.notifyme.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.config.constants.RedisKeyConstants;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service Layer for User Notification Preferences in Redis.
 */
@Slf4j
@Service
public class UserPreferenceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public UserPreferenceService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Saves or updates user notification preferences in Redis Cache.
     */
    public void savePreference(UserPreference preference) {
        String cacheKey = RedisKeyConstants.USER_PREFS_KEY_PREFIX + preference.getUserId();
        redisTemplate.opsForValue().set(cacheKey, preference);
        log.info("Preferences saved in Redis for user '{}'", preference.getUserId());
    }

    /**
     * Retrieves user preferences from Redis Cache.
     */
    public Optional<UserPreference> getPreference(String userId) {
        String cacheKey = RedisKeyConstants.USER_PREFS_KEY_PREFIX + userId;
        Object rawCached = redisTemplate.opsForValue().get(cacheKey);

        if (rawCached == null) {
            return Optional.empty();
        }

        try {
            if (rawCached instanceof UserPreference up) {
                return Optional.of(up);
            }
            return Optional.of(objectMapper.convertValue(rawCached, UserPreference.class));
        } catch (Exception e) {
            log.warn("Error deserializing preferences from Redis for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
