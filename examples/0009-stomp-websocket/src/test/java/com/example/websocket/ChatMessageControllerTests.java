package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChatMessageControllerTests {
    @Test
    void mapsApplicationMessageToTopicEvent() {
        ChatMessageController controller = new ChatMessageController();

        ChatMessageController.ChatEvent event = controller.send(
                new ChatMessageController.ChatCommand("alice", "hello"));

        assertEquals("alice", event.sender());
        assertEquals("hello", event.text());
    }
}
