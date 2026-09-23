package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

class DevTokenHandshakeInterceptorTests {

    private final DevTokenHandshakeInterceptor interceptor =
            new DevTokenHandshakeInterceptor("local-dev-token");

    @Test
    void acceptsExpectedToken() {
        ServerHttpRequest request = request("http://localhost:8080/ws/heartbeat?token=local-dev-token");

        assertTrue(interceptor.beforeHandshake(request, mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class), new HashMap<>()));
    }

    @Test
    void rejectsMissingToken() {
        ServerHttpRequest request = request("http://localhost:8080/ws/heartbeat");

        assertFalse(interceptor.beforeHandshake(request, mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class), new HashMap<>()));
    }

    private ServerHttpRequest request(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        return request;
    }
}
