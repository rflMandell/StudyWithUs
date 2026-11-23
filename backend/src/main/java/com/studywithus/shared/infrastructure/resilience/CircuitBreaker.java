package com.studywithus.shared.infrastructure.resilience;

import com.studywithus.shared.domain.DomainException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt = Instant.EPOCH;
    private final Counter rejectedCounter;
    private final Counter failureCounter;

    public CircuitBreaker(String name, int failureThreshold, Duration openDuration, MeterRegistry meterRegistry) {
        this.name = name;
        this.failureThreshold = Math.max(failureThreshold, 1);
        this.openDuration = openDuration != null ? openDuration : Duration.ofSeconds(30);
        if (meterRegistry != null) {
            meterRegistry.gauge("studywithus_circuit_breaker_state",
                    io.micrometer.core.instrument.Tags.of("name", name),
                    this,
                    cb -> switch (cb.getState()) {
                        case CLOSED -> 0;
                        case HALF_OPEN -> 1;
                        case OPEN -> 2;
                    });
            this.rejectedCounter = Counter.builder("studywithus_circuit_breaker_rejected_total")
                    .tag("name", name)
                    .description("Total calls short-circuited while circuit breaker is OPEN")
                    .register(meterRegistry);
            this.failureCounter = Counter.builder("studywithus_circuit_breaker_failures_total")
                    .tag("name", name)
                    .description("Total infrastructure failures recorded by circuit breaker")
                    .register(meterRegistry);
        } else {
            this.rejectedCounter = null;
            this.failureCounter = null;
        }
    }

    public <T> T execute(Supplier<T> supplier) {
        State current = evaluateState();
        if (current == State.OPEN) {
            if (rejectedCounter != null) {
                rejectedCounter.increment();
            }
            throw new DomainException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "STORAGE_CIRCUIT_OPEN",
                    "Service '" + name + "' is temporarily unavailable"
            );
        }

        try {
            T result = supplier.get();
            onSuccess();
            return result;
        } catch (DomainException ex) {
            // Business/validation exceptions (4xx) do not trip the infrastructure circuit breaker
            if (ex.getStatus().is5xxServerError()) {
                onFailure();
            }
            throw ex;
        } catch (RuntimeException ex) {
            onFailure();
            throw ex;
        }
    }

    public void executeVoid(Runnable runnable) {
        execute(() -> {
            runnable.run();
            return null;
        });
    }

    public State getState() {
        return evaluateState();
    }

    private State evaluateState() {
        State current = state.get();
        if (current == State.OPEN && Instant.now().isAfter(openedAt.plus(openDuration))) {
            state.compareAndSet(State.OPEN, State.HALF_OPEN);
        }
        return state.get();
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    private void onFailure() {
        if (failureCounter != null) {
            failureCounter.increment();
        }
        int failures = consecutiveFailures.incrementAndGet();
        if (state.get() == State.HALF_OPEN || failures >= failureThreshold) {
            openedAt = Instant.now();
            state.set(State.OPEN);
        }
    }
}
