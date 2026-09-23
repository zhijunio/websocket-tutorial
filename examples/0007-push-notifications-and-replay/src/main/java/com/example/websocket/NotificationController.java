package com.example.websocket;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @PostMapping("/users/{userId}/notifications")
    public NotificationService.Notification publish(@PathVariable String userId,
                                                     @RequestBody PublishRequest request) {
        return notifications.publish(userId, request.text());
    }

    @GetMapping("/users/{userId}/notifications/unread")
    public List<NotificationService.Notification> unread(@PathVariable String userId) {
        return notifications.unread(userId);
    }

    @PostMapping("/users/{userId}/notifications/{notificationId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable String userId,
                                            @PathVariable String notificationId) {
        return notifications.acknowledge(userId, notificationId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    record PublishRequest(String text) {
    }
}
