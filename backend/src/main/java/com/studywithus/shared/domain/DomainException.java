package com.studywithus.shared.domain;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public DomainException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public DomainException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }
}
