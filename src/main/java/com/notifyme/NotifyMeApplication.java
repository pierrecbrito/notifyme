package com.notifyme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação NotifyMe.
 * 
 * A anotação @SpringBootApplication habilita:
 * 1. @Configuration: Permite registrar beans customizados no contexto do Spring.
 * 2. @EnableAutoConfiguration: Configura automaticamente RabbitMQ, Redis e Web MVC
 *    com base nas dependências do pom.xml e nas propriedades do application.yml.
 * 3. @ComponentScan: Varre o pacote 'com.notifyme' e sub-pacotes em busca de 
 *    Controllers, Services, Repositories e Components.
 */
@SpringBootApplication
public class NotifyMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyMeApplication.class, args);
    }
}
