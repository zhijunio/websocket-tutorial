package com.example.websocket;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String instanceId = "local";
    private String tenantId = "demo";
    private Duration ticketTtl = Duration.ofSeconds(30);
    private Duration presenceTtl = Duration.ofSeconds(45);
    private Duration heartbeatInterval = Duration.ofSeconds(15);
    private int outboundQueueCapacity = 100;
    private int maxMessageBytes = 8192;
    private int maxConnectionsPerUser = 5;
    private int maxConnectionsPerInstance = 10000;
    private int maxConnectionsPerIp = 100;
    private int maxConnectionsPerTenant = 20000;
    private int maxHandshakesPerMinute = 120;
    private int maxNotificationRate = 600;
    private int streamMaxLength = 100000;
    private int maxOutboxAttempts = 10;
    private String jwtSecret;
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:8080", "http://localhost:8081", "http://localhost:8082"));

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Duration getTicketTtl() { return ticketTtl; }
    public void setTicketTtl(Duration ticketTtl) { this.ticketTtl = ticketTtl; }
    public Duration getPresenceTtl() { return presenceTtl; }
    public void setPresenceTtl(Duration presenceTtl) { this.presenceTtl = presenceTtl; }
    public Duration getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(Duration heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public int getOutboundQueueCapacity() { return outboundQueueCapacity; }
    public void setOutboundQueueCapacity(int outboundQueueCapacity) { this.outboundQueueCapacity = outboundQueueCapacity; }
    public int getMaxMessageBytes() { return maxMessageBytes; }
    public void setMaxMessageBytes(int maxMessageBytes) { this.maxMessageBytes = maxMessageBytes; }
    public int getMaxConnectionsPerUser() { return maxConnectionsPerUser; }
    public void setMaxConnectionsPerUser(int maxConnectionsPerUser) { this.maxConnectionsPerUser = maxConnectionsPerUser; }
    public int getMaxConnectionsPerInstance() { return maxConnectionsPerInstance; }
    public void setMaxConnectionsPerInstance(int maxConnectionsPerInstance) { this.maxConnectionsPerInstance = maxConnectionsPerInstance; }
    public int getMaxConnectionsPerIp() { return maxConnectionsPerIp; }
    public void setMaxConnectionsPerIp(int maxConnectionsPerIp) { this.maxConnectionsPerIp = maxConnectionsPerIp; }
    public int getMaxConnectionsPerTenant() { return maxConnectionsPerTenant; }
    public void setMaxConnectionsPerTenant(int maxConnectionsPerTenant) { this.maxConnectionsPerTenant = maxConnectionsPerTenant; }
    public int getMaxHandshakesPerMinute() { return maxHandshakesPerMinute; }
    public void setMaxHandshakesPerMinute(int maxHandshakesPerMinute) { this.maxHandshakesPerMinute = maxHandshakesPerMinute; }
    public int getMaxNotificationRate() { return maxNotificationRate; }
    public void setMaxNotificationRate(int maxNotificationRate) { this.maxNotificationRate = maxNotificationRate; }
    public int getStreamMaxLength() { return streamMaxLength; }
    public void setStreamMaxLength(int streamMaxLength) { this.streamMaxLength = streamMaxLength; }
    public int getMaxOutboxAttempts() { return maxOutboxAttempts; }
    public void setMaxOutboxAttempts(int maxOutboxAttempts) { this.maxOutboxAttempts = maxOutboxAttempts; }
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
}
