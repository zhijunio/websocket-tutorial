package com.example.websocket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class LoadTestRunner {
    private LoadTestRunner() {
    }

    public static void main(String[] args) throws Exception {
        int clients = integerArg(args, 0, 100);
        int messagesPerSecond = integerArg(args, 1, 10);
        int durationSeconds = integerArg(args, 2, 60);
        String endpoint = stringArg(args, 3, "ws://localhost:8080/ws/load");
        new LoadTestRunner().run(clients, messagesPerSecond, durationSeconds, endpoint);
    }

    void run(int clients, int messagesPerSecond, int durationSeconds, String endpoint) throws Exception {
        if (clients < 1 || messagesPerSecond < 1 || durationSeconds < 1) {
            throw new IllegalArgumentException("clients, rate and duration must be positive");
        }
        Metrics metrics = new Metrics();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Math.min(clients, 32));
        List<LoadClient> loadClients = new ArrayList<>();
        CountDownLatch opened = new CountDownLatch(clients);
        HttpClient httpClient = HttpClient.newHttpClient();
        for (int i = 0; i < clients; i++) {
            LoadClient loadClient = new LoadClient(httpClient, URI.create(endpoint), metrics, opened);
            loadClients.add(loadClient);
            loadClient.connect();
        }
        if (!opened.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("not all clients opened within 30 seconds");
        }
        long start = System.nanoTime();
        long periodMillis = Math.max(1_000L / messagesPerSecond, 1L);
        long messagesPerClient = Math.multiplyExact((long) messagesPerSecond, durationSeconds);
        CountDownLatch sentAll = new CountDownLatch(clients);
        for (LoadClient loadClient : loadClients) {
            loadClient.configure(messagesPerClient, sentAll);
            scheduler.scheduleAtFixedRate(loadClient::send, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
        }
        sentAll.await(Duration.ofSeconds(durationSeconds + 5L).toMillis(), TimeUnit.MILLISECONDS);
        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);
        waitForPending(metrics, Duration.ofSeconds(5));
        for (LoadClient loadClient : loadClients) {
            loadClient.close();
        }
        metrics.print(clients, messagesPerSecond, durationSeconds, start);
    }

    private static void waitForPending(Metrics metrics, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (metrics.pending.get() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    private static int integerArg(String[] args, int index, int fallback) {
        return args.length > index ? Integer.parseInt(args[index]) : fallback;
    }

    private static String stringArg(String[] args, int index, String fallback) {
        return args.length > index ? args[index] : fallback;
    }

    static final class LoadClient implements WebSocket.Listener {
        private final HttpClient httpClient;
        private final URI endpoint;
        private final Metrics metrics;
        private final CountDownLatch opened;
        private final Map<String, Long> pending = new ConcurrentHashMap<>();
        private final StringBuilder fragments = new StringBuilder();
        private volatile WebSocket socket;
        private long remaining;
        private CountDownLatch sentAll;

        LoadClient(HttpClient httpClient, URI endpoint, Metrics metrics, CountDownLatch opened) {
            this.httpClient = httpClient;
            this.endpoint = endpoint;
            this.metrics = metrics;
            this.opened = opened;
        }

        void connect() {
            httpClient.newWebSocketBuilder().buildAsync(endpoint, this)
                    .whenComplete((value, error) -> {
                        if (error != null) {
                            metrics.errors.incrementAndGet();
                        }
                    });
        }

        void configure(long messagesToSend, CountDownLatch sentAll) {
            this.remaining = messagesToSend;
            this.sentAll = sentAll;
        }

        void send() {
            if (remaining == 0) {
                return;
            }
            WebSocket current = socket;
            if (current == null) {
                return;
            }
            remaining--;
            String id = Long.toString(metrics.sent.incrementAndGet());
            String payload = payload(id);
            pending.put(id, System.nanoTime());
            metrics.pending.incrementAndGet();
            current.sendText(payload, true).exceptionally(error -> {
                pending.remove(id);
                metrics.pending.decrementAndGet();
                metrics.errors.incrementAndGet();
                return null;
            });
            if (remaining == 0) {
                sentAll.countDown();
            }
        }

        void close() {
            WebSocket current = socket;
            if (current != null) {
                current.sendClose(WebSocket.NORMAL_CLOSURE, "benchmark done");
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            opened.countDown();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            fragments.append(data);
            if (last) {
                String text = fragments.toString();
                fragments.setLength(0);
                int delimiter = text.indexOf('|');
                if (delimiter <= 0) {
                    metrics.errors.incrementAndGet();
                    webSocket.request(1);
                    return CompletableFuture.completedFuture(null);
                }
                String id = text.substring(0, delimiter);
                Long sentAt = pending.remove(id);
                if (sentAt != null) {
                    metrics.latencies.add(System.nanoTime() - sentAt);
                    metrics.received.incrementAndGet();
                    metrics.pending.decrementAndGet();
                } else {
                    metrics.errors.incrementAndGet();
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            metrics.errors.incrementAndGet();
        }

        private String payload(String id) {
            String prefix = id + "|" + System.nanoTime() + "|";
            return prefix + "x".repeat(Math.max(0, 256 - prefix.length()));
        }
    }

    static final class Metrics {
        final AtomicLong sent = new AtomicLong();
        final AtomicLong received = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        final AtomicLong pending = new AtomicLong();
        final List<Long> latencies = java.util.Collections.synchronizedList(new ArrayList<>());

        void print(int clients, int rate, int duration, long start) {
            long elapsedSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
            long expected = (long) clients * rate * duration;
            long lost = Math.max(0, sent.get() - received.get());
            double errorRate = sent.get() == 0 ? 0 : (double) lost / sent.get();
            System.out.printf("clients=%d targetRate=%d durationSeconds=%d%n", clients, rate, duration);
            System.out.printf("expected=%d sent=%d received=%d lost=%d errors=%d pending=%d throughputPerSecond=%d errorRate=%.4f%n",
                    expected, sent.get(), received.get(), lost, errors.get(), pending.get(),
                    received.get() / elapsedSeconds, errorRate);
            System.out.printf("p50Ms=%d p95Ms=%d samples=%d%n",
                    LatencyStats.percentileMillis(latencies, 0.50),
                    LatencyStats.percentileMillis(latencies, 0.95), latencies.size());
        }
    }
}
