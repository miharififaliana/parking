package com.parking.service;

import com.parking.config.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Calcul des montants de stationnement (§3.2 CDC).
 *
 * <p>Formule : {@code montant = CEIL(durée_en_minutes / 60) × tarif_horaire}</p>
 * <p>Durée minimum facturée : 1 heure.</p>
 */
public class TarifService {

    private final AppConfig appConfig;

    public TarifService() {
        this(AppConfig.getInstance());
    }

    TarifService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Calcule le montant dû entre la date d'entrée et la date de sortie.
     */
    public BigDecimal calculerMontant(LocalDateTime dateEntree, LocalDateTime dateSortie) {
        if (dateEntree == null || dateSortie == null) {
            throw new IllegalArgumentException("Les dates d'entrée et de sortie sont obligatoires");
        }
        if (dateSortie.isBefore(dateEntree)) {
            throw new IllegalArgumentException("La date de sortie ne peut pas être antérieure à l'entrée");
        }

        long minutes = calculerDureeMinutes(dateEntree, dateSortie);
        long heuresFacturees = calculerHeuresFacturees(minutes);
        return appConfig.getTarifHoraire()
                .multiply(BigDecimal.valueOf(heuresFacturees))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Durée de stationnement en minutes (arrondi vers le bas).
     */
    public long calculerDureeMinutes(LocalDateTime dateEntree, LocalDateTime dateSortie) {
        return Duration.between(dateEntree, dateSortie).toMinutes();
    }

    /**
     * Nombre d'heures facturées selon la règle CEIL(min/60), minimum 1 heure.
     */
    public long calculerHeuresFacturees(long dureeMinutes) {
        if (dureeMinutes < 0) {
            throw new IllegalArgumentException("La durée ne peut pas être négative");
        }
        long heures = (long) Math.ceil(dureeMinutes / 60.0);
        return Math.max(1, heures);
    }

    public BigDecimal getTarifHoraire() {
        return appConfig.getTarifHoraire();
    }
}
