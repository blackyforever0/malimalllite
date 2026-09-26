package com.malimall.backend.exception;

/** Levée quand un téléversement de photo est tenté sans identifiants Supabase Storage configurés. */
public class StorageNonConfigureException extends RuntimeException {
    public StorageNonConfigureException(String message) {
        super(message);
    }
}
