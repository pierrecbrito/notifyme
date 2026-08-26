package com.notifyme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for NotifyMe.
 * 
 * The @SpringBootApplication annotation enables:
 * 1. @Configuration: Allows registering custom beans into the Spring application context.
 * 2. @EnableAutoConfiguration: Automatically configures RabbitMQ, Redis, and Web MVC
 *    based on pom.xml dependencies and application.yml settings.
 * 3. @ComponentScan: Scans the 'com.notifyme' package and subpackages for
 *    Controllers, Services, Repositories, and Components.
 */
@SpringBootApplication
public class NotifyMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyMeApplication.class, args);
    }
}
