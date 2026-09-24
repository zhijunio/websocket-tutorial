package com.example.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

public class TicketHandshakeInterceptor implements HandshakeInterceptor {
    static final String PRINCIPAL = TicketHandshakeInterceptor.class.getName() + ".principal";

    private final OneTimeTicketService tickets;
    private final AppProperties properties;
    private final HandshakeRateLimiter rateLimiter;

    public TicketHandshakeInterceptor(OneTimeTicketService tickets, AppProperties properties, HandshakeRateLimiter rateLimiter) {
        this.tickets = tickets;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest && !rateLimiter.allow(servletRequest.getServletRequest())) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            attributes.put("clientIp", servletRequest.getServletRequest().getRemoteAddr());
        }
        if (request.getPrincipal() != null) {
            attributes.put(PRINCIPAL, request.getPrincipal());
            attributes.put("tenantId", properties.getTenantId());
            attributes.put("clientType", "browser-session");
            return true;
        }
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
        TicketClaims claims = tickets.consume(ticket);
        if (claims == null || !"websocket".equals(claims.audience())) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Principal principal = () -> claims.subject();
        attributes.put(PRINCIPAL, principal);
        attributes.put("tenantId", claims.tenantId());
        attributes.put("clientType", claims.clientType());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }
}
