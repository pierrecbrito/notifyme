package com.notifyme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * AWS SDK v2 Configuration for DynamoDB.
 * 
 * Provides:
 * 1. DynamoDbClient (low-level): manages HTTP connections and credentials.
 * 2. DynamoDbEnhancedClient (high-level): maps Java classes directly to NoSQL tables.
 */
@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.dynamodb.endpoint:http://localhost:8000}")
    private String endpoint;

    @Value("${aws.dynamodb.access-key:local-access-key}")
    private String accessKey;

    @Value("${aws.dynamodb.secret-key:local-secret-key}")
    private String secretKey;

    /**
     * Low-level DynamoDB client.
     * Supports both Local DynamoDB (via endpointOverride) and production AWS cloud.
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));

        // Override endpoint if provided (e.g., http://localhost:8000 for DynamoDB Local)
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    /**
     * High-level Enhanced Client.
     * Enables object-oriented CRUD operations with annotations such as @DynamoDbBean.
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
