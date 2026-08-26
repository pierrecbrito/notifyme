package com.notifyme.delivery.consumer;

import com.notifyme.delivery.service.DeliveryService;
import com.notifyme.domain.event.DeliveryTaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * AMQP Delivery Worker Consumer: Listens to the 'notifyme.delivery.tasks' queue
 * and delegates multi-channel dispatch to DeliveryService.
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
        log.debug("Delivery task message received from queue: {}", task.taskId());
        deliveryService.processDelivery(task);
    }
}
