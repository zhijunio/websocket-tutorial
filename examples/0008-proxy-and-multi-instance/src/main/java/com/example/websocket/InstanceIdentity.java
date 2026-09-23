package com.example.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstanceIdentity {
    private final String value;

    public InstanceIdentity(@Value("${app.instance-id:local}") String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
