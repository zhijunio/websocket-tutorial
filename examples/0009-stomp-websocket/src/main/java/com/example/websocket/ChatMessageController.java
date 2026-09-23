package com.example.websocket;

import java.time.Instant;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {

    @MessageMapping("/chat.send")
    @SendTo("/topic/room")
    public ChatEvent send(ChatCommand command) {
        return new ChatEvent(command.sender(), command.text(), Instant.now());
    }

    record ChatCommand(String sender, String text) {
    }

    record ChatEvent(String sender, String text, Instant sentAt) {
    }
}
