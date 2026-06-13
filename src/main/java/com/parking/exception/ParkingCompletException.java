package com.parking.exception;

/**
 * Levée lorsqu'aucune place libre n'est disponible (parking complet).
 */
public class ParkingCompletException extends RuntimeException {

    public ParkingCompletException(String message) {
        super(message);
    }
}
