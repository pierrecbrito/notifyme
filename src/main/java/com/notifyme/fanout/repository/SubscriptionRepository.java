package com.notifyme.fanout.repository;

import com.notifyme.domain.model.UserSubscription;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.ArrayList;
import java.util.List;

/**
 * DynamoDB Repository for User Subscriptions (user_subscriptions table).
 * 
 * Uses DynamoDbEnhancedClient for high-speed queries indexed by Partition Key (channel_id).
 */
@Slf4j
@Repository
public class SubscriptionRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<UserSubscription> table;

    public SubscriptionRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(UserSubscription.TABLE_NAME, TableSchema.fromBean(UserSubscription.class));
    }

    /**
     * Automatically creates the table in DynamoDB Local if it does not exist yet.
     */
    @PostConstruct
    public void initTable() {
        try {
            table.createTable();
            log.info("Table '{}' created successfully in DynamoDB", UserSubscription.TABLE_NAME);
        } catch (ResourceInUseException e) {
            log.debug("Table '{}' already exists in DynamoDB", UserSubscription.TABLE_NAME);
        } catch (Exception e) {
            log.warn("Notice while verifying/creating table '{}': {}", UserSubscription.TABLE_NAME, e.getMessage());
        }
    }

    /**
     * Saves or updates a user subscription to a creator channel.
     */
    public void save(UserSubscription subscription) {
        table.putItem(subscription);
    }

    /**
     * Returns a paginated PageIterable of subscribers for a given channel.
     * Ideal for processing large channels in chunks without exhausting memory.
     */
    public PageIterable<UserSubscription> querySubscribersPaged(String channelId, int pageSize) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(channelId).build()
        );

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .limit(pageSize)
                .build();

        return table.query(request);
    }

    /**
     * Returns the list of active user IDs subscribed to a given channel.
     */
    public List<String> findActiveSubscriberIdsByChannelId(String channelId) {
        List<String> userIds = new ArrayList<>();

        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(channelId).build()
        );

        PageIterable<UserSubscription> pagedResults = table.query(r -> r.queryConditional(queryConditional));

        for (Page<UserSubscription> page : pagedResults) {
            for (UserSubscription subscription : page.items()) {
                if (Boolean.TRUE.equals(subscription.getActive())) {
                    userIds.add(subscription.getUserId());
                }
            }
        }

        return userIds;
    }
}
