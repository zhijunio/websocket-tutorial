package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisTicketStoreTests {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;

    @BeforeAll
    static void setUp() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getFirstMappedPort());
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void redisLuaConsumptionAllowsOnlyOneConcurrentWinner() throws Exception {
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        OneTimeTicketService service = new OneTimeTicketService(new RedisTicketStore(redis, new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()), 30);
        IssuedTicket issued = service.issue("alice", "tenant-a", "browser");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<TicketClaims> winners = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < 8; i++) {
            executor.submit(() -> {
                ready.countDown();
                start.await();
                TicketClaims claims = service.consume(issued.value());
                if (claims != null) {
                    winners.add(claims);
                }
                return null;
            });
        }

        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        assertEquals(1, winners.size());
    }

    @Test
    void presenceIsTenantIsolatedAndExpiresByLease() throws Exception {
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        AppProperties properties = new AppProperties();
        properties.setInstanceId("test-instance");
        properties.setPresenceTtl(java.time.Duration.ofSeconds(1));
        PresenceService presence = new PresenceService(redis, properties);

        presence.connect("tenant-a", "alice", "browser", "connection-a");
        presence.connect("tenant-b", "alice", "browser", "connection-b");

        assertEquals(1, presence.find("tenant-a", "alice", null, null).size());
        assertEquals(0, presence.find("tenant-a", "bob", null, null).size());
        Thread.sleep(1200);
        assertEquals(0, presence.find("tenant-a", "alice", null, null).size());
    }

    @Test
    void connectionAdmissionEnforcesSharedQuotaAndReleasesLease() {
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        AppProperties properties = new AppProperties();
        properties.setInstanceId("admission-test");
        properties.setMaxConnectionsPerUser(1);
        properties.setMaxConnectionsPerTenant(10);
        properties.setMaxConnectionsPerInstance(10);
        properties.setMaxConnectionsPerIp(10);
        ConnectionAdmissionService admission = new ConnectionAdmissionService(redis, properties);

        assertTrue(admission.tryAcquire("tenant-a", "alice", "192.0.2.1", "connection-a"));
        assertEquals(false, admission.tryAcquire("tenant-a", "alice", "192.0.2.1", "connection-b"));
        admission.release("tenant-a", "alice", "192.0.2.1", "connection-a");
        assertTrue(admission.tryAcquire("tenant-a", "alice", "192.0.2.1", "connection-b"));
        admission.release("tenant-a", "alice", "192.0.2.1", "connection-b");
    }
}
