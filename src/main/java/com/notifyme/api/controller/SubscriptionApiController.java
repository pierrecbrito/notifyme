package com.notifyme.api.controller;

import com.notifyme.domain.model.UserPreference;
import com.notifyme.domain.model.UserSubscription;
import com.notifyme.fanout.repository.SubscriptionRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Controller de Gerenciamento de Inscrições e Preferências de Usuários.
 * 
 * Permite que clientes e o aplicativo cadastrem seguidores em canais
 * e configurem as preferências de notificação salvas no Redis.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class SubscriptionApiController {

    private static final String USER_PREFS_KEY_PREFIX = "user:prefs:";

    private final SubscriptionRepository subscriptionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SubscriptionApiController(
            SubscriptionRepository subscriptionRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Inscreve um usuário em um canal do YouTube (salva no DynamoDB).
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<String> subscribeUser(@RequestBody SubscribeRequest request) {
        if (request.getChannelId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body("channelId e userId são obrigatórios");
        }

        UserSubscription subscription = UserSubscription.builder()
                .channelId(request.getChannelId())
                .userId(request.getUserId())
                .active(true)
                .createdAt(Instant.now())
                .build();

        subscriptionRepository.save(subscription);
        log.info("Usuário '{}' inscrito com sucesso no canal '{}'", request.getUserId(), request.getChannelId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(String.format("Usuário '%s' inscrito no canal '%s'", request.getUserId(), request.getChannelId()));
    }

    /**
     * Retorna a lista de seguidores ativos de um canal do YouTube.
     */
    @GetMapping("/subscriptions/{channelId}")
    public ResponseEntity<List<String>> getSubscribers(@PathVariable String channelId) {
        List<String> subscribers = subscriptionRepository.findActiveSubscriberIdsByChannelId(channelId);
        return ResponseEntity.ok(subscribers);
    }

    /**
     * Define ou atualiza as preferências de notificação do usuário (salva no Redis Cache).
     */
    @PostMapping("/users/{userId}/preferences")
    public ResponseEntity<String> saveUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreference preference
    ) {
        preference.setUserId(userId);
        String cacheKey = USER_PREFS_KEY_PREFIX + userId;

        redisTemplate.opsForValue().set(cacheKey, preference);
        log.info("Preferências atualizadas no Redis para o usuário '{}': {}", userId, preference);

        return ResponseEntity.ok("Preferências salvas com sucesso no Redis para o usuário: " + userId);
    }

    /**
     * Consulta as preferências de um usuário gravadas no Redis.
     */
    @GetMapping("/users/{userId}/preferences")
    public ResponseEntity<Object> getUserPreferences(@PathVariable String userId) {
        String cacheKey = USER_PREFS_KEY_PREFIX + userId;
        Object prefs = redisTemplate.opsForValue().get(cacheKey);

        if (prefs == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma preferência em cache para o usuário: " + userId);
        }

        return ResponseEntity.ok(prefs);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscribeRequest {
        private String channelId;
        private String userId;
    }
}
