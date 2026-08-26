package com.notifyme.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Modelo de Preferências do Usuário (Cached no Redis).
 * 
 * Contém os canais escolhidos pelo usuário e os dados de contato
 * necessários para efetuar o disparo imediato sem consultar banco SQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference implements Serializable {

    private String userId;

    /**
     * Canais que o usuário escolheu para receber alertas (ex: PUSH, EMAIL, SMS).
     */
    @Builder.Default
    private Set<NotificationChannel> enabledChannels = Set.of(NotificationChannel.PUSH);

    /**
     * E-mail para disparo via SendGrid / SES.
     */
    private String email;

    /**
     * Número de telefone no padrão internacional (ex: +5511999998888) para SMS via Twilio.
     */
    private String phoneNumber;

    /**
     * Tokens de dispositivos móveis para envio de Push via Firebase FCM.
     */
    @Builder.Default
    private List<String> deviceTokens = Collections.emptyList();

    /**
     * Verifica se um determinado canal está ativo para este usuário.
     */
    public boolean isChannelEnabled(NotificationChannel channel) {
        return enabledChannels != null && enabledChannels.contains(channel);
    }
}
