package com.notifyme.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for subscribing a user to a YouTube creator channel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String channelId;
    private String userId;
}
