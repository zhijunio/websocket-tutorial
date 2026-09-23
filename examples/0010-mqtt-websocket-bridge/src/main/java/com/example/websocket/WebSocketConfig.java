package com.example.websocket;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final DeviceWebSocketHandler handler;

    public WebSocketConfig(DeviceWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/devices")
                .addInterceptors(new DeviceIdInterceptor())
                .setAllowedOriginPatterns("*");
    }

    static class DeviceIdInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler handler, Map<String, Object> attributes) {
            String deviceId = request.getURI().getQuery();
            if (deviceId == null || !deviceId.matches("[A-Za-z0-9_-]{1,32}")) {
                return false;
            }
            attributes.put("deviceId", deviceId);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Exception exception) {
        }
    }
}
