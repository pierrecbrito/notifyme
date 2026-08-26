package com.notifyme.fanout.consumer;

import com.notifyme.domain.event.VideoPublishedEvent;
import com.notifyme.fanout.service.FanoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * AMQP Fan-out Consumer: Listens to the 'notifyme.video.published' queue
 * and delegates chunking and deduplication logic to FanoutService.
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
        log.debug("Video published message received from queue: {}", event.videoId());
        fanoutService.processFanout(event);
    }
}
