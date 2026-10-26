package com.example.crud.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Bounded, per-instance fixed windows. Never keys on user-controlled account names. */
public final class LoginRateLimiter {
    private final int attempts;
    private final int capacity;
    private final long windowNanos;
    private final LongSupplier clock;
    private final Map<String, Window> clients = new HashMap<>();

    public LoginRateLimiter(int attempts, int capacity, Duration window, LongSupplier clock) {
        if (attempts < 1 || capacity < 1 || window.isNegative() || window.isZero()
                || window.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Limites de login devem ser positivos; janela maxima de um dia");
        }
        this.attempts = attempts;
        this.capacity = capacity;
        this.windowNanos = window.toNanos();
        this.clock = clock;
    }

    public synchronized Decision acquire(String client) {
        long now = clock.getAsLong();
        Window window = clients.get(client);
        if (window != null && now - window.started >= windowNanos) {
            clients.remove(client);
            window = null;
        }
        if (window == null) {
            if (clients.size() >= capacity) {
                clients.values().removeIf(value -> now - value.started >= windowNanos);
                // Fail closed if all slots remain active; never evict a blocked client.
                if (clients.size() >= capacity) return new Decision(false, seconds(windowNanos));
            }
            window = new Window(now);
            clients.put(client, window);
        }
        if (window.used >= attempts) {
            return new Decision(false, seconds(windowNanos - (now - window.started)));
        }
        window.used++;
        return new Decision(true, 0);
    }

    private static long seconds(long nanos) {
        return Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L);
    }

    private static final class Window {
        final long started;
        int used;
        Window(long started) { this.started = started; }
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {}
}
