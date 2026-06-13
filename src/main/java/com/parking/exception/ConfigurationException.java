package com.parking.exception;

/**
 * Levée lorsque le fichier {@code config.properties} est introuvable,
 * illisible ou contient des valeurs invalides.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
