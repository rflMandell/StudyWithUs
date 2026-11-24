package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.infrastructure.resilience.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Primary
@Component
public class ResilientObjectStorageAdapter implements ObjectStoragePort {

    private final ObjectStoragePort delegate;
    private final CircuitBreaker circuitBreaker;

    @Autowired
    public ResilientObjectStorageAdapter(
            List<ObjectStoragePort> delegates,
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Value("${app.media.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${app.media.circuit-breaker.open-duration-seconds:30}") long openDurationSeconds
    ) {
        this.delegate = delegates.stream()
                .filter(d -> !(d instanceof ResilientObjectStorageAdapter))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No ObjectStoragePort delegate bean configured"));
        this.circuitBreaker = new CircuitBreaker(
                "object-storage",
                failureThreshold,
                Duration.ofSeconds(openDurationSeconds),
                meterRegistry
        );
    }

    public ResilientObjectStorageAdapter(ObjectStoragePort delegate, CircuitBreaker circuitBreaker) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public PresignedUploadResult generatePresignedUploadUrl(String storageKey, String contentType, long contentLength, Duration expiration) {
        return circuitBreaker.execute(() -> delegate.generatePresignedUploadUrl(storageKey, contentType, contentLength, expiration));
    }

    @Override
    public boolean exists(String storageKey) {
        return circuitBreaker.execute(() -> delegate.exists(storageKey));
    }

    @Override
    public StoredObjectMetadata getMetadata(String storageKey) {
        return circuitBreaker.execute(() -> delegate.getMetadata(storageKey));
    }

    @Override
    public byte[] download(String storageKey) {
        return circuitBreaker.execute(() -> delegate.download(storageKey));
    }

    @Override
    public void upload(String storageKey, byte[] data, String contentType) {
        circuitBreaker.executeVoid(() -> delegate.upload(storageKey, data, contentType));
    }

    @Override
    public void delete(String storageKey) {
        circuitBreaker.executeVoid(() -> delegate.delete(storageKey));
    }

    @Override
    public String getPublicUrl(String storageKey) {
        return delegate.getPublicUrl(storageKey);
    }

    @Override
    public StorageHealth checkHealth() {
        StorageHealth base = delegate.checkHealth();
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            return new StorageHealth(false, base.provider(), base.bucketOrPath(), base.latencyMs(), "Circuit breaker OPEN");
        }
        return base;
    }

    public CircuitBreaker.State getCircuitState() {
        return circuitBreaker.getState();
    }
}
