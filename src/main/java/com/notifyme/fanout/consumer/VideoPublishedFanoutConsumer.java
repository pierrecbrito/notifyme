package com.notifyme.fanout.consumer;

import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.fanout.service.FanoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor AMQP de Fan-out: Ouve a fila 'notifyme.video.published'
 * e delega o processamento de fatiamento e deduplicação para o FanoutService.
 */
@Slf4j
@Component
public class VideoPublishedFanoutConsumer {

    private final FanoutService fanoutService;

    public VideoPublishedFanoutConsumer(FanoutService fanoutService) {
        this.fanoutService = fanoutService;
    }

    @RabbitListener(queues = "${notifyme.queues.video-published:notifyme.video.published}")
    public void onVideoPublished(VideoPublishedEvent event) {
        log.debug("Mensagem de vídeo publicado recebida da fila: {}", event.videoId());
        fanoutService.processFanout(event);
    }
}
