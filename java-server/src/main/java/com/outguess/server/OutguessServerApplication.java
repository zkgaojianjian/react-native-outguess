package com.outguess.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Outguess服务端解码器主应用
 */
@SpringBootApplication
@EnableConfigurationProperties
public class OutguessServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutguessServerApplication.class, args);
    }
}