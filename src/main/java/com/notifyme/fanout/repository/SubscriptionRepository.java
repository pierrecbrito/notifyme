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
 * Repositório DynamoDB para Inscrições de Usuários (user_subscriptions).
 * 
 * Utiliza o DynamoDbEnhancedClient para realizar consultas de alta velocidade
 * indexadas por Partition Key (channel_id).
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
     * Cria a tabela automaticamente no DynamoDB Local se ela ainda não existir.
     */
    @PostConstruct
    public void initTable() {
        try {
            table.createTable();
            log.info("Tabela '{}' criada com sucesso no DynamoDB", UserSubscription.TABLE_NAME);
        } catch (ResourceInUseException e) {
            log.debug("Tabela '{}' já existe no DynamoDB", UserSubscription.TABLE_NAME);
        } catch (Exception e) {
            log.warn("Aviso ao verificar/criar tabela '{}': {}", UserSubscription.TABLE_NAME, e.getMessage());
        }
    }

    /**
     * Salva ou atualiza uma inscrição de usuário em um canal.
     */
    public void save(UserSubscription subscription) {
        table.putItem(subscription);
    }

    /**
     * Retorna um PageIterable paginado de inscritos para um canal.
     * Ideal para o Fan-out processar grandes canais em lotes (chunks) sem estourar memória.
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
     * Retorna a lista de IDs de todos os usuários ativos inscritos em um canal.
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
