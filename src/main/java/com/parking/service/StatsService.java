package com.parking.service;

import com.parking.dao.TicketDao;
import com.parking.model.StatsSnapshot;
import com.parking.model.Ticket;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/**
 * Statistiques de fréquentation et recettes (§4.6 CDC).
 */
public class StatsService {

    private static final int RECETTES_JOURS = 30;

    private final TicketDao ticketDao;

    public StatsService() {
        this(new TicketDao());
    }

    StatsService(TicketDao ticketDao) {
        this.ticketDao = ticketDao;
    }

    public StatsSnapshot getSnapshot() {
        LocalDate today = LocalDate.now();
        LocalDateTime debutJour = today.atStartOfDay();
        LocalDateTime fin = debutJour.plusDays(1);
        LocalDateTime debutSemaine = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime debutAnnee = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime debutRecettes = today.minusDays(RECETTES_JOURS - 1L).atStartOfDay();

        int entreesJour = ticketDao.countEntreesBetween(debutJour, fin);
        int entreesSemaine = ticketDao.countEntreesBetween(debutSemaine, fin);
        int entreesMois = ticketDao.countEntreesBetween(debutMois, fin);
        int entreesAnnee = ticketDao.countEntreesBetween(debutAnnee, fin);
        double dureeMoyenne = ticketDao.avgDureeMinutesPayesBetween(debutAnnee, fin);
        BigDecimal recetteTotale = ticketDao.sumMontantPayeBetween(debutAnnee, fin);
        List<StatsSnapshot.RecetteJour> recettesParJour = ticketDao.sumMontantPayeGroupByDay(debutRecettes, fin);
        Map<Integer, Integer> entreesParHeure = ticketDao.countEntreesGroupByHour(debutMois, fin);

        return new StatsSnapshot(entreesJour, entreesSemaine, entreesMois, entreesAnnee,
                dureeMoyenne, recetteTotale, recettesParJour, entreesParHeure);
    }

    public List<Ticket> getTicketsPourExport() {
        return ticketDao.findAll();
    }
}
