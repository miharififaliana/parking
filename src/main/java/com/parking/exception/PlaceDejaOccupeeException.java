package com.parking.exception;

/**
 * Levée lors d'une tentative d'attribution sur une place déjà occupée.
 */
public class PlaceDejaOccupeeException extends RuntimeException {

    public PlaceDejaOccupeeException(String message) {
        super(message);
    }
}
