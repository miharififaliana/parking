package com.parking.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Emplacement physique du parking (table {@code places}).
 */
public class Place {

    private int idPlace;
    private int numeroPlace;
    private StatutPlace statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Place() {
    }

    public Place(int idPlace, int numeroPlace, StatutPlace statut) {
        this.idPlace = idPlace;
        this.numeroPlace = numeroPlace;
        this.statut = statut;
    }

    public int getIdPlace() {
        return idPlace;
    }

    public void setIdPlace(int idPlace) {
        this.idPlace = idPlace;
    }

    public int getNumeroPlace() {
        return numeroPlace;
    }

    public void setNumeroPlace(int numeroPlace) {
        this.numeroPlace = numeroPlace;
    }

    public StatutPlace getStatut() {
        return statut;
    }

    public void setStatut(StatutPlace statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isLibre() {
        return statut == StatutPlace.LIBRE;
    }

    public boolean isOccupee() {
        return statut == StatutPlace.OCCUPEE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Place place)) return false;
        return idPlace == place.idPlace;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPlace);
    }

    @Override
    public String toString() {
        return "Place{id=" + idPlace + ", numero=" + numeroPlace + ", statut=" + statut + "}";
    }
}
