package com.resumematcher.exception;

public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException() {
        super("The analysis service is temporarily unavailable. Please try again later.");
    }
}