package com.parking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Instantané des indicateurs du tableau de bord (§4.5 CDC).
 */
public class DashboardSnapshot {

    private final int totalPlaces;
    private final int placesLibres;
    private final int placesOccupees;
    private final double tauxOccupation;
    private final BigDecimal recetteJour;
    private final BigDecimal recetteSemaine;
    private final BigDecimal recetteMois;
    private final List<MouvementResume> derniersMouvements;
    private final LocalDateTime horodatage;

    public DashboardSnapshot(int totalPlaces, int placesLibres, int placesOccupees,
                             BigDecimal recetteJour, BigDecimal recetteSemaine, BigDecimal recetteMois,
                             List<MouvementResume> derniersMouvements, LocalDateTime horodatage) {
        this.totalPlaces = totalPlaces;
        this.placesLibres = placesLibres;
        this.placesOccupees = placesOccupees;
        this.tauxOccupation = totalPlaces > 0 ? (placesOccupees * 100.0 / totalPlaces) : 0.0;
        this.recetteJour = recetteJour;
        this.recetteSemaine = recetteSemaine;
        this.recetteMois = recetteMois;
        this.derniersMouvements = List.copyOf(derniersMouvements);
        this.horodatage = horodatage;
    }

    public int getTotalPlaces() {
        return totalPlaces;
    }

    public int getPlacesLibres() {
        return placesLibres;
    }

    public int getPlacesOccupees() {
        return placesOccupees;
    }

    public double getTauxOccupation() {
        return tauxOccupation;
    }

    public BigDecimal getRecetteJour() {
        return recetteJour;
    }

    public BigDecimal getRecetteSemaine() {
        return recetteSemaine;
    }

    public BigDecimal getRecetteMois() {
        return recetteMois;
    }

    public List<MouvementResume> getDerniersMouvements() {
        return derniersMouvements;
    }

    public LocalDateTime getHorodatage() {
        return horodatage;
    }

    /**
     * Ligne affichée dans la liste des derniers mouvements.
     */
    public static class MouvementResume {

        private final String type;
        private final String numeroTicket;
        private final int numeroPlace;
        private final String immatriculation;
        private final LocalDateTime dateHeure;
        private final BigDecimal montant;

        public MouvementResume(String type, String numeroTicket, int numeroPlace,
                               String immatriculation, LocalDateTime dateHeure, BigDecimal montant) {
            this.type = type;
            this.numeroTicket = numeroTicket;
            this.numeroPlace = numeroPlace;
            this.immatriculation = immatriculation;
            this.dateHeure = dateHeure;
            this.montant = montant;
        }

        public String getType() {
            return type;
        }

        public String getNumeroTicket() {
            return numeroTicket;
        }

        public int getNumeroPlace() {
            return numeroPlace;
        }

        public String getImmatriculation() {
            return immatriculation;
        }

        public LocalDateTime getDateHeure() {
            return dateHeure;
        }

        public BigDecimal getMontant() {
            return montant;
        }
    }
}
