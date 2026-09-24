package com.example.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * The explicit configuration keeps HTTP sessions shared when Nginx routes a
 * request or a WebSocket handshake to another application instance.
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "${SESSION_REDIS_NAMESPACE:ws:session}")
public class SessionConfig {
}
