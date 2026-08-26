package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;

/**
 * Contrato (Interface) para Provedores de Notificação.
 * 
 * Permite aplicar o padrão de projeto Strategy: cada canal de envio
 * (FCM, SendGrid, Twilio) implementa esta interface e é acionado dinamicamente
 * de acordo com as preferências ativas do usuário.
 */
public interface NotificationProvider {

    /**
     * Retorna o canal suportado por este provedor (PUSH, EMAIL, SMS).
     */
    NotificationChannel getChannel();

    /**
     * Executa o disparo da notificação para o usuário.
     * 
     * @param task Dados da notificação do vídeo publicado.
     * @param preference Dados de contato e preferências do usuário (em memória via Redis).
     */
    void send(DeliveryTaskEvent task, UserPreference preference);
}
