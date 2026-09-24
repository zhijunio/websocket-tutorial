package com.example.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler handler;
    private final OneTimeTicketService tickets;
    private final AppProperties properties;
    private final HandshakeRateLimiter rateLimiter;

    public WebSocketConfig(NotificationWebSocketHandler handler, OneTimeTicketService tickets, AppProperties properties,
                           HandshakeRateLimiter rateLimiter) {
        this.handler = handler;
        this.tickets = tickets;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
                .addInterceptors(new TicketHandshakeInterceptor(tickets, properties, rateLimiter))
                .setAllowedOriginPatterns(properties.getAllowedOrigins().toArray(String[]::new));
    }
}
