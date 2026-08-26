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
 * Controller de Gerenciamento de Inscrições e Preferências de Usuários.
 * 
 * Camada REST que expõe contratos padronizados via ApiResponse<T> e delega para os Services.
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
     * Inscreve um usuário em um canal do YouTube (salva no DynamoDB via SubscriptionService).
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<ApiResponse<Void>> subscribeUser(@RequestBody SubscribeRequestDto request) {
        try {
            subscriptionService.subscribe(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(String.format("Usuário '%s' inscrito com sucesso no canal '%s'",
                            request.getUserId(), request.getChannelId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Retorna a lista de seguidores ativos de um canal do YouTube.
     */
    @GetMapping("/subscriptions/{channelId}")
    public ResponseEntity<ApiResponse<List<String>>> getSubscribers(@PathVariable String channelId) {
        List<String> subscribers = subscriptionService.getActiveSubscribers(channelId);
        return ResponseEntity.ok(ApiResponse.ok("Seguidores recuperados com sucesso", subscribers));
    }

    /**
     * Define ou atualiza as preferências de notificação do usuário (salva no Redis Cache).
     */
    @PostMapping("/users/{userId}/preferences")
    public ResponseEntity<ApiResponse<Void>> saveUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreference preference
    ) {
        preference.setUserId(userId);
        userPreferenceService.savePreference(preference);

        return ResponseEntity.ok(ApiResponse.ok("Preferências salvas com sucesso no Redis para o usuário: " + userId));
    }

    /**
     * Consulta as preferências de um usuário gravadas no Redis.
     */
    @GetMapping("/users/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserPreference>> getUserPreferences(@PathVariable String userId) {
        Optional<UserPreference> preference = userPreferenceService.getPreference(userId);

        if (preference.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Nenhuma preferência em cache para o usuário: " + userId));
        }

        return ResponseEntity.ok(ApiResponse.ok("Preferências encontradas", preference.get()));
    }
}
