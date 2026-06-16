package com.parking.model;

/**
 * État d'un ticket de stationnement (§8 CDC).
 */
public enum StatutTicket {

    ACTIF,
    PAYE,
    ANNULE;

    /**
     * Convertit une valeur ENUM MySQL en constante Java.
     *
     * @param value valeur lue en base (ex. {@code "ACTIF"})
     * @return la constante correspondante
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static StatutTicket fromDbValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Statut de ticket null ou vide");
        }
        return StatutTicket.valueOf(value.trim().toUpperCase());
    }
}
