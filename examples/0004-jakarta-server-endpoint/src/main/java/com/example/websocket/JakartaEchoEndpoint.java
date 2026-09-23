package com.example.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws/jakarta-echo")
public class JakartaEchoEndpoint {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Jakarta WebSocket opened: " + session.getId());
    }

    @OnMessage
    public String onMessage(String message) {
        return "echo: " + message;
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Jakarta WebSocket closed: " + session.getId());
    }
}
