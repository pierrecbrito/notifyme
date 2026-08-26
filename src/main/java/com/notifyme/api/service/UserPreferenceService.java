package com.notifyme.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.config.constants.RedisKeyConstants;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Camada de Serviço para Gerenciamento de Preferências do Usuário no Redis.
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
     * Salva ou atualiza as preferências do usuário no Redis Cache.
     */
    public void savePreference(UserPreference preference) {
        String cacheKey = RedisKeyConstants.USER_PREFS_KEY_PREFIX + preference.getUserId();
        redisTemplate.opsForValue().set(cacheKey, preference);
        log.info("Preferências salvas no Redis para o usuário '{}'", preference.getUserId());
    }

    /**
     * Busca as preferências de um usuário no Redis.
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
            log.warn("Erro ao desserializar preferências do Redis para o usuário {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
