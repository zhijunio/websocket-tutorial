package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class OneTimeTicketServiceTests {

    @Test
    void aTicketCanBeConsumedOnlyOnce() {
        OneTimeTicketService service = new OneTimeTicketService(new InMemoryTicketStore(), 30);

        IssuedTicket issued = service.issue("alice", "demo", "browser");

        assertEquals("alice", service.consume(issued.value()).subject());
        assertNull(service.consume(issued.value()));
    }

    @Test
    void concurrentConsumersOnlyAllowOneWinner() throws Exception {
        OneTimeTicketService service = new OneTimeTicketService(new InMemoryTicketStore(), 30);
        IssuedTicket issued = service.issue("alice", "demo", "browser");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<TicketClaims> winners = new CopyOnWriteArrayList<>();

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
        assertNotNull(winners.getFirst());
    }
}
