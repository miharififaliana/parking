package com.parking.model;

/**
 * État d'une place de parking (§3.1 CDC).
 */
public enum StatutPlace {

    LIBRE,
    OCCUPEE;

    /**
     * Convertit une valeur ENUM MySQL en constante Java.
     *
     * @param value valeur lue en base (ex. {@code "LIBRE"})
     * @return la constante correspondante
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static StatutPlace fromDbValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Statut de place null ou vide");
        }
        return StatutPlace.valueOf(value.trim().toUpperCase());
    }
}
