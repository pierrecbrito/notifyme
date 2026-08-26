package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provedor de Notificações por E-mail via SendGrid / AWS SES.
 */
@Slf4j
@Component
public class EmailSendGridProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(DeliveryTaskEvent task, UserPreference preference) {
        String email = preference.getEmail();

        if (email == null || email.isBlank()) {
            log.warn("[EMAIL SendGrid] Usuário {} não possui e-mail cadastrado. Ignorando envio.",
                    task.userId());
            return;
        }

        // Em produção, aqui montamos o Mail/SendGrid API request com template HTML
        log.info("[EMAIL SendGrid] ✉️ Enviando e-mail para <{}> (Usuário {}). Assunto: 'Novo vídeo: {}' -> Link: {}",
                email, task.userId(), task.title(), task.videoUrl());
    }
}
