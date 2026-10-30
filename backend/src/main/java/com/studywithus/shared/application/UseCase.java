package com.studywithus.shared.application;

/**
 * Marker interface for application use cases.
 */
public interface UseCase<I, O> {
    O execute(I input);
}
