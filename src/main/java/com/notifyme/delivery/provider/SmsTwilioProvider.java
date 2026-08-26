package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provedor de Notificações por SMS via Twilio.
 */
@Slf4j
@Component
public class SmsTwilioProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(DeliveryTaskEvent task, UserPreference preference) {
        String phoneNumber = preference.getPhoneNumber();

        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("[SMS Twilio] Usuário {} não possui telefone cadastrado. Ignorando envio.",
                    task.userId());
            return;
        }

        // Em produção, aqui usamos o Twilio Message.creator(...)
        String messageBody = String.format("NotifyMe: Novo vídeo postado! '%s' - Assista agora: %s",
                task.title(), task.videoUrl());

        log.info("[SMS Twilio] 📱 Enviando SMS para {} (Usuário {}). Mensagem: '{}'",
                phoneNumber, task.userId(), messageBody);
    }
}
