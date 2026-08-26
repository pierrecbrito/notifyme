package com.notifyme.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Beans do Jackson para parsing XML e JSON.
 */
@Configuration
public class JacksonConfig {

    /**
     * Registra o XmlMapper no container de IoC do Spring,
     * permitindo que seja injetado via construtor nos serviços.
     */
    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }
}
