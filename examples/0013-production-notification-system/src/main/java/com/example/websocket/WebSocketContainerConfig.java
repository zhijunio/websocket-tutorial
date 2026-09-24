package com.example.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class WebSocketContainerConfig {
    @Bean
    ServletServerContainerFactoryBean webSocketContainer(AppProperties properties) {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(properties.getMaxMessageBytes());
        container.setMaxBinaryMessageBufferSize(1024);
        return container;
    }
}
