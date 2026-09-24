package com.example.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WsTicketController {
    private final OneTimeTicketService tickets;
    private final AppProperties properties;

    public WsTicketController(OneTimeTicketService tickets, AppProperties properties) {
        this.tickets = tickets;
        this.properties = properties;
    }

    @PostMapping("/api/ws-ticket")
    public Map<String, Object> issue(Principal principal) {
        IssuedTicket ticket = tickets.issue(principal.getName(), properties.getTenantId(), "browser");
        return Map.of("ticket", ticket.value(), "expiresAt", ticket.expiresAt());
    }
}
