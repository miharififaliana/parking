package com.parking.exception;

/**
 * Levée lorsqu'une entrée utilisateur ne respecte pas les règles de validation (§5.4 CDC).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
