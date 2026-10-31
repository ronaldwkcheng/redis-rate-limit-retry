package com.ronald.rest.client.exception;

import org.springframework.http.HttpStatusCode;

public class RetryableServerException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public RetryableServerException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

}
