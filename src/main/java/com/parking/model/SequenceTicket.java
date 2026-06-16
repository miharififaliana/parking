package com.parking.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Compteur de numérotation annuelle des tickets (table {@code sequence_tickets}).
 */
public class SequenceTicket {

    private int annee;
    private int derniereSequence;
    private LocalDateTime updatedAt;

    public SequenceTicket() {
    }

    public SequenceTicket(int annee, int derniereSequence) {
        this.annee = annee;
        this.derniereSequence = derniereSequence;
    }

    public int getAnnee() {
        return annee;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public int getDerniereSequence() {
        return derniereSequence;
    }

    public void setDerniereSequence(int derniereSequence) {
        this.derniereSequence = derniereSequence;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SequenceTicket that)) return false;
        return annee == that.annee;
    }

    @Override
    public int hashCode() {
        return Objects.hash(annee);
    }

    @Override
    public String toString() {
        return "SequenceTicket{annee=" + annee + ", derniereSequence=" + derniereSequence + "}";
    }
}
