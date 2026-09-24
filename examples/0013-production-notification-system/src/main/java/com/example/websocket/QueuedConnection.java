package com.example.websocket;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.PingMessage;

final class QueuedConnection {
    private final WebSocketSession session;
    private final ArrayBlockingQueue<WebSocketMessage<?>> queue;
    private final ExecutorService sender = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("ws-send-", 0).factory());
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean closeAfterDrain = new AtomicBoolean();
    private final Runnable onFailure;

    QueuedConnection(WebSocketSession session, int capacity, Runnable onFailure) {
        this.session = session;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.onFailure = onFailure;
        sender.submit(this::drain);
    }

    boolean offer(String payload) {
        return offer(new TextMessage(payload));
    }

    boolean offerPing() {
        return offer(new PingMessage());
    }

    private boolean offer(WebSocketMessage<?> message) {
        if (closed.get() || !session.isOpen() || !queue.offer(message)) {
            return false;
        }
        return true;
    }

    int queueSize() {
        return queue.size();
    }

    boolean closeAfter(String payload) {
        closeAfterDrain.set(true);
        if (!offer(payload)) {
            closeAfterDrain.set(false);
            return false;
        }
        return true;
    }

    void close() {
        if (closed.compareAndSet(false, true)) {
            sender.shutdownNow();
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException ignored) {
                // The connection is already being removed.
            }
        }
    }

    private void drain() {
        try {
            while (!closed.get()) {
                WebSocketMessage<?> message = queue.poll(1, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }
                if (!session.isOpen()) {
                    onFailure.run();
                    return;
                }
                session.sendMessage(message);
                if (closeAfterDrain.get() && queue.isEmpty()) {
                    close();
                    return;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            onFailure.run();
        }
    }
}
