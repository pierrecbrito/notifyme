package com.notifyme.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for XML and JSON parsing beans.
 */
@Configuration
public class JacksonConfig {

    /**
     * Registers XmlMapper as a Spring bean in the IoC container,
     * allowing it to be injected via constructors into services.
     */
    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }
}
