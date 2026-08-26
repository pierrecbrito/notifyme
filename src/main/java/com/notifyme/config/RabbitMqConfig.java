package com.notifyme.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Topology Configuration (Exchanges, Queues, Bindings, and JSON Converter).
 * 
 * Topology:
 * 1. Main Exchange (notifyme.exchange):
 *    - Route 'video.published' -> Queue 'notifyme.video.published'
 *    - Route 'delivery.task'   -> Queue 'notifyme.delivery.tasks'
 * 
 * 2. Dead Letter Exchange (notifyme.dlx):
 *    - Route 'delivery.dlq'    -> Queue 'notifyme.delivery.dlq' (stores unrecoverable failures)
 */
@Configuration
public class RabbitMqConfig {

    @Value("${notifyme.exchanges.main:notifyme.exchange}")
    private String mainExchangeName;

    @Value("${notifyme.exchanges.dlx:notifyme.dlx}")
    private String dlxExchangeName;

    @Value("${notifyme.queues.video-published:notifyme.video.published}")
    private String videoPublishedQueueName;

    @Value("${notifyme.queues.delivery-tasks:notifyme.delivery.tasks}")
    private String deliveryTasksQueueName;

    @Value("${notifyme.queues.delivery-dlq:notifyme.delivery.dlq}")
    private String deliveryDlqName;

    @Value("${notifyme.routing-keys.video-published:video.published}")
    private String videoPublishedRoutingKey;

    @Value("${notifyme.routing-keys.delivery-task:delivery.task}")
    private String deliveryTaskRoutingKey;

    @Value("${notifyme.routing-keys.delivery-dlq:delivery.dlq}")
    private String deliveryDlqRoutingKey;

    // --- 1. EXCHANGES ---

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(mainExchangeName, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(dlxExchangeName, true, false);
    }

    // --- 2. QUEUES ---

    @Bean
    public Queue videoPublishedQueue() {
        return QueueBuilder.durable(videoPublishedQueueName).build();
    }

    /**
     * Delivery Tasks Queue.
     * Configured with Dead Letter Exchange (DLX): if task processing fails after
     * all configured retries, RabbitMQ automatically moves the message to the DLQ.
     */
    @Bean
    public Queue deliveryTasksQueue() {
        return QueueBuilder.durable(deliveryTasksQueueName)
                .deadLetterExchange(dlxExchangeName)
                .deadLetterRoutingKey(deliveryDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue deliveryDlq() {
        return QueueBuilder.durable(deliveryDlqName).build();
    }

    // --- 3. BINDINGS (CONNECTIONS BETWEEN EXCHANGES AND QUEUES) ---

    @Bean
    public Binding bindingVideoPublished(Queue videoPublishedQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(videoPublishedQueue)
                .to(mainExchange)
                .with(videoPublishedRoutingKey);
    }

    @Bean
    public Binding bindingDeliveryTasks(Queue deliveryTasksQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(deliveryTasksQueue)
                .to(mainExchange)
                .with(deliveryTaskRoutingKey);
    }

    @Bean
    public Binding bindingDeliveryDlq(Queue deliveryDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(deliveryDlq)
                .to(dlxExchange)
                .with(deliveryDlqRoutingKey);
    }

    // --- 4. JSON SERIALIZER ---

    /**
     * Allows Java DTOs and Records to be published and consumed as clean JSON across queues
     * without manual string serialization and deserialization.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
