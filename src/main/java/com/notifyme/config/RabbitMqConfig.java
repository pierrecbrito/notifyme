package com.notifyme.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da Topologia do RabbitMQ (Exchanges, Queues, Bindings e Serializador JSON).
 * 
 * Topologia:
 * 1. Exchange Principal (notifyme.exchange):
 *    - Rota 'video.published' -> Fila 'notifyme.video.published'
 *    - Rota 'delivery.task'   -> Fila 'notifyme.delivery.tasks'
 * 
 * 2. Dead Letter Exchange (notifyme.dlx):
 *    - Rota 'delivery.dlq'    -> Fila 'notifyme.delivery.dlq' (armazena falhas definitivas)
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

    // --- 2. FILAS (QUEUES) ---

    @Bean
    public Queue videoPublishedQueue() {
        return QueueBuilder.durable(videoPublishedQueueName).build();
    }

    /**
     * Fila de Tarefas de Envio.
     * Configurada com Dead Letter Exchange (DLX): se o processamento falhar após
     * todos os retries, o RabbitMQ move a mensagem automaticamente para a DLQ.
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

    // --- 3. BINDINGS (LIGAÇÕES ENTRE EXCHANGE E FILAS) ---

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

    // --- 4. SERIALIZADOR JSON ---

    /**
     * Permite trafegar DTOs e Records Java nas filas diretamente como JSON legível,
     * sem precisar serializar e desserializar strings manualmente.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
