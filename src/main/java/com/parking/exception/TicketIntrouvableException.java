package com.parking.exception;

/**
 * Levée lorsqu'aucun ticket actif ne correspond à la recherche.
 */
public class TicketIntrouvableException extends RuntimeException {

    public TicketIntrouvableException(String message) {
        super(message);
    }
}
