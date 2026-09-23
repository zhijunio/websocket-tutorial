package com.example.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    private final HeartbeatWebSocketHandler heartbeatHandler;
    private final DevTokenHandshakeInterceptor tokenInterceptor;

    public WebSocketConfig(HeartbeatWebSocketHandler heartbeatHandler) {
        this.heartbeatHandler = heartbeatHandler;
        this.tokenInterceptor = new DevTokenHandshakeInterceptor("local-dev-token");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(heartbeatHandler, "/ws/heartbeat")
                .addInterceptors(tokenInterceptor)
                .setAllowedOrigins("http://localhost:8080");
    }
}
