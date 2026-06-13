package com.parking.exception;

/**
 * Levée lors d'une erreur de connexion ou d'exécution JDBC.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
