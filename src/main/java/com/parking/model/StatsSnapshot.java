package com.parking.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Données agrégées pour l'écran statistiques (§4.6 CDC).
 */
public class StatsSnapshot {

    private final int entreesJour;
    private final int entreesSemaine;
    private final int entreesMois;
    private final int entreesAnnee;
    private final double dureeMoyenneMinutes;
    private final BigDecimal recetteTotale;
    private final List<RecetteJour> recettesParJour;
    private final Map<Integer, Integer> entreesParHeure;

    public StatsSnapshot(int entreesJour, int entreesSemaine, int entreesMois, int entreesAnnee,
                         double dureeMoyenneMinutes, BigDecimal recetteTotale,
                         List<RecetteJour> recettesParJour, Map<Integer, Integer> entreesParHeure) {
        this.entreesJour = entreesJour;
        this.entreesSemaine = entreesSemaine;
        this.entreesMois = entreesMois;
        this.entreesAnnee = entreesAnnee;
        this.dureeMoyenneMinutes = dureeMoyenneMinutes;
        this.recetteTotale = recetteTotale;
        this.recettesParJour = recettesParJour;
        this.entreesParHeure = entreesParHeure;
    }

    public int getEntreesJour() { return entreesJour; }
    public int getEntreesSemaine() { return entreesSemaine; }
    public int getEntreesMois() { return entreesMois; }
    public int getEntreesAnnee() { return entreesAnnee; }
    public double getDureeMoyenneMinutes() { return dureeMoyenneMinutes; }
    public BigDecimal getRecetteTotale() { return recetteTotale; }
    public List<RecetteJour> getRecettesParJour() { return recettesParJour; }
    public Map<Integer, Integer> getEntreesParHeure() { return entreesParHeure; }

    public record RecetteJour(String jour, BigDecimal montant) {
    }
}
