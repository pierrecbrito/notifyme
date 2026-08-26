package com.notifyme.delivery.consumer;

import com.notifyme.delivery.service.DeliveryService;
import com.notifyme.domain.event.DeliveryTaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Worker Consumidor AMQP: Ouve a fila 'notifyme.delivery.tasks'
 * e delega o disparo multicanal para o DeliveryService.
 */
@Slf4j
@Component
public class DeliveryTaskConsumer {

    private final DeliveryService deliveryService;

    public DeliveryTaskConsumer(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = "${notifyme.queues.delivery-tasks:notifyme.delivery.tasks}")
    public void onDeliveryTask(DeliveryTaskEvent task) {
        log.debug("Mensagem de tarefa de entrega recebida da fila: {}", task.taskId());
        deliveryService.processDelivery(task);
    }
}
