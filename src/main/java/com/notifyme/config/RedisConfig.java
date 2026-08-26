package com.notifyme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuração do Redis Cache.
 * 
 * Configura o RedisTemplate com:
 * - Chaves em String legível (StringRedisSerializer)
 * - Valores em JSON padronizado (GenericJackson2JsonRedisSerializer)
 * 
 * Isso evita caracteres binários estranhos (ex: \xac\xed\x00\x05) ao inspecionar
 * as chaves no Redis CLI ou em ferramentas de visualização.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // Configuração de Serialização para Chaves Simples
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);

        // Configuração de Serialização para Hashes
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
