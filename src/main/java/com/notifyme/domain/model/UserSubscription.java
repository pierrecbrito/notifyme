package com.notifyme.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Entidade DynamoDB: Mapeia a tabela 'user_subscriptions'.
 */
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class UserSubscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "user_subscriptions";

    private String channelId;
    private String userId;
    private Instant createdAt;
    private Boolean active;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("channel_id")
    public String getChannelId() {
        return channelId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("user_id")
    public String getUserId() {
        return userId;
    }

    @DynamoDbAttribute("created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("active")
    public Boolean getActive() {
        return active;
    }
}
