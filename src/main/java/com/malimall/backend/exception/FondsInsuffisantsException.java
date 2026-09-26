package com.malimall.backend.exception;

public class FondsInsuffisantsException extends RuntimeException {
    public FondsInsuffisantsException(String message) {
        super(message);
    }
}
