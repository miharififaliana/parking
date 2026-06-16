package com.parking.service;

import com.parking.dao.PlaceDao;
import com.parking.dao.TicketDao;
import com.parking.model.DashboardSnapshot;
import com.parking.model.Place;
import com.parking.model.StatutPlace;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agrégation des données du tableau de bord (§4.5 CDC).
 */
public class DashboardService {

    private static final int DERNiers_MOUVEMENTS_LIMIT = 10;

    private final PlaceDao placeDao;
    private final TicketDao ticketDao;

    public DashboardService() {
        this(new PlaceDao(), new TicketDao());
    }

    DashboardService(PlaceDao placeDao, TicketDao ticketDao) {
        this.placeDao = placeDao;
        this.ticketDao = ticketDao;
    }

    /**
     * Construit un instantané complet pour l'affichage du tableau de bord.
     */
    public DashboardSnapshot getSnapshot() {
        int libres = placeDao.countByStatut(StatutPlace.LIBRE);
        int occupees = placeDao.countByStatut(StatutPlace.OCCUPEE);
        int total = libres + occupees;

        LocalDate today = LocalDate.now();
        LocalDateTime debutJour = today.atStartOfDay();
        LocalDateTime debutSemaine = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fin = debutJour.plusDays(1);

        BigDecimal recetteJour = ticketDao.sumMontantPayeBetween(debutJour, fin);
        BigDecimal recetteSemaine = ticketDao.sumMontantPayeBetween(debutSemaine, fin);
        BigDecimal recetteMois = ticketDao.sumMontantPayeBetween(debutMois, fin);

        Map<Integer, Integer> numeroParPlaceId = placeDao.findAll().stream()
                .collect(Collectors.toMap(Place::getIdPlace, Place::getNumeroPlace));

        List<DashboardSnapshot.MouvementResume> mouvements = ticketDao.findDerniersMouvements(DERNiers_MOUVEMENTS_LIMIT)
                .stream()
                .map(ticket -> toMouvementResume(ticket, numeroParPlaceId))
                .toList();

        return new DashboardSnapshot(
                total, libres, occupees,
                recetteJour, recetteSemaine, recetteMois,
                mouvements,
                LocalDateTime.now()
        );
    }

    private DashboardSnapshot.MouvementResume toMouvementResume(Ticket ticket, Map<Integer, Integer> numeroParPlaceId) {
        boolean sortie = ticket.getStatut() == StatutTicket.PAYE && ticket.getDateSortie() != null;
        String type = sortie ? "Sortie" : "Entrée";
        LocalDateTime dateHeure = sortie ? ticket.getDateSortie() : ticket.getDateEntree();
        int numeroPlace = numeroParPlaceId.getOrDefault(ticket.getIdPlace(), ticket.getIdPlace());

        return new DashboardSnapshot.MouvementResume(
                type,
                ticket.getNumeroTicket(),
                numeroPlace,
                ticket.getImmatriculation(),
                dateHeure,
                ticket.getMontant()
        );
    }
}
