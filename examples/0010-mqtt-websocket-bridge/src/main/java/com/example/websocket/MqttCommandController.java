package com.example.websocket;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@ConditionalOnBean(MqttBridgeService.class)
public class MqttCommandController {
    private final MqttBridgeService bridge;

    public MqttCommandController(MqttBridgeService bridge) {
        this.bridge = bridge;
    }

    @PostMapping("/{deviceId}/commands")
    public ResponseEntity<Map<String, String>> publish(@PathVariable String deviceId,
                                                        @RequestBody CommandRequest request)
            throws MqttException {
        bridge.publishCommand(deviceId, request.payload());
        return ResponseEntity.accepted().body(Map.of("topic", TopicMapper.commandTopic(deviceId)));
    }

    record CommandRequest(String payload) {
    }
}
