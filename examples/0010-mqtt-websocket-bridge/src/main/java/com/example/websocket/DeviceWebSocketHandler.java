package com.example.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Component
public class DeviceWebSocketHandler extends AbstractWebSocketHandler {
    private final DeviceConnectionRegistry registry;

    public DeviceWebSocketHandler(DeviceConnectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.register(deviceId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(deviceId(session), session);
    }

    private String deviceId(WebSocketSession session) {
        Object value = session.getAttributes().get("deviceId");
        return value == null ? "unknown" : value.toString();
    }
}
