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
 * Configuração do AWS SDK v2 para DynamoDB.
 * 
 * Cria os Beans:
 * 1. DynamoDbClient (baixo nível): gerencia a conexão HTTP e credenciais.
 * 2. DynamoDbEnhancedClient (alto nível): mapeia classes Java diretamente para tabelas NoSQL.
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
     * Cliente de baixo nível do DynamoDB.
     * Suporta tanto o DynamoDB Local (via endpointOverride) quanto a AWS real.
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));

        // Se houver um endpoint configurado (ex: http://localhost:8000), sobrescreve o padrão da AWS
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    /**
     * Cliente de alto nível (Enhanced Client).
     * Permite fazer CRUD orientado a objetos com anotações como @DynamoDbBean.
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
