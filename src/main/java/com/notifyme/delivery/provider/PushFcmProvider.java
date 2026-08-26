package com.notifyme.delivery.provider;

import com.notifyme.domain.event.DeliveryTaskEvent;
import com.notifyme.domain.model.NotificationChannel;
import com.notifyme.domain.model.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provedor de Notificações Push via Firebase Cloud Messaging (FCM).
 */
@Slf4j
@Component
public class PushFcmProvider implements NotificationProvider {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(DeliveryTaskEvent task, UserPreference preference) {
        List<String> deviceTokens = preference.getDeviceTokens();

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("[FCM PUSH] Usuário {} não possui tokens de dispositivos cadastrados. Ignorando envio.",
                    task.userId());
            return;
        }

        // Em produção, aqui usamos o FirebaseMessaging.getInstance().sendEachForMulticast(message)
        log.info("[FCM PUSH] ✅ Disparando Push para {} dispositivo(s) do usuário {}. Vídeo: '{}' ({})",
                deviceTokens.size(), task.userId(), task.title(), task.videoUrl());
    }
}
