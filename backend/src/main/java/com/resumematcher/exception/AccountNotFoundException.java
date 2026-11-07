package com.resumematcher.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Account no longer exists");
    }
}