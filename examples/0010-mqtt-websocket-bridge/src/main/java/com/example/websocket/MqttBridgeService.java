package com.example.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttBridgeService {
    private final DeviceConnectionRegistry registry;
    private final String brokerUrl;
    private final String clientId;
    private MqttClient client;

    public MqttBridgeService(DeviceConnectionRegistry registry,
                             @Value("${mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
                             @Value("${mqtt.client-id:websocket-bridge}") String clientId) {
        this.registry = registry;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    @PostConstruct
    void start() throws MqttException {
        client = new MqttClient(brokerUrl, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
        client.subscribe("devices/+/events", 1, inboundListener());
    }

    public void publishCommand(String deviceId, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        message.setQos(1);
        client.publish(TopicMapper.commandTopic(deviceId), message);
    }

    @PreDestroy
    void stop() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }

    private IMqttMessageListener inboundListener() {
        return (topic, message) -> TopicMapper.deviceIdFromEventTopic(topic)
                .ifPresent(deviceId -> registry.sendToDevice(deviceId,
                        new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8)));
    }
}
