package com.parking.model;

/**
 * Rôle d'un utilisateur du système (§4.1 CDC).
 */
public enum Role {

    GERANT,
    CAISSIER,
    ADMIN;

    /**
     * Convertit une valeur ENUM MySQL en constante Java.
     *
     * @param value valeur lue en base (ex. {@code "ADMIN"})
     * @return la constante correspondante
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static Role fromDbValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rôle utilisateur null ou vide");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}
