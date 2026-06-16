package com.parking.service;

import com.parking.config.DatabaseConfig;
import com.parking.dao.LogDao;
import com.parking.dao.PlaceDao;
import com.parking.dao.SequenceTicketDao;
import com.parking.dao.TicketDao;
import com.parking.exception.ParkingCompletException;
import com.parking.exception.PlaceDejaOccupeeException;
import com.parking.exception.TicketIntrouvableException;
import com.parking.exception.ValidationException;
import com.parking.model.Log;
import com.parking.model.Place;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Orchestration des flux d'entrée et de sortie (§3.3 / §3.4 / §9 CDC).
 */
public class TicketService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    /** Plaque alphanumérique, espaces et tirets autorisés (max 20 car. — §5.4 CDC). */
    private static final Pattern IMMATRICULATION_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 \\-]{1,19}$");

    private final PlaceDao placeDao;
    private final TicketDao ticketDao;
    private final SequenceTicketDao sequenceTicketDao;
    private final LogDao logDao;
    private final TarifService tarifService;
    private final SessionManager sessionManager;

    public TicketService() {
        this(new PlaceDao(), new TicketDao(), new SequenceTicketDao(),
                new LogDao(), new TarifService(), SessionManager.getInstance());
    }

    TicketService(PlaceDao placeDao, TicketDao ticketDao, SequenceTicketDao sequenceTicketDao,
                  LogDao logDao, TarifService tarifService, SessionManager sessionManager) {
        super();
        this.placeDao = placeDao;
        this.ticketDao = ticketDao;
        this.sequenceTicketDao = sequenceTicketDao;
        this.logDao = logDao;
        this.tarifService = tarifService;
        this.sessionManager = sessionManager;
    }

    TicketService(DatabaseConfig databaseConfig, PlaceDao placeDao, TicketDao ticketDao,
                  SequenceTicketDao sequenceTicketDao, LogDao logDao,
                  TarifService tarifService, SessionManager sessionManager) {
        super(databaseConfig);
        this.placeDao = placeDao;
        this.ticketDao = ticketDao;
        this.sequenceTicketDao = sequenceTicketDao;
        this.logDao = logDao;
        this.tarifService = tarifService;
        this.sessionManager = sessionManager;
    }

    /**
     * Propose la place libre de plus petit numéro (§3.1 CDC).
     */
    public Optional<Place> proposerPlaceLibre() {
        return placeDao.findFirstLibre();
    }

    /**
     * Enregistre une entrée : attribution automatique, occupation, génération ticket (§3.3 CDC).
     */
    public Ticket enregistrerEntree(String immatriculation) {
        String immatNormalisee = validerEtNormaliserImmatriculation(immatriculation);
        int idUser = requireUtilisateurConnecte();

        return executeInTransaction(conn ->
                enregistrerEntree(conn, immatNormalisee, idUser, LocalDateTime.now()));
    }

    /**
     * Enregistre une sortie par numéro de ticket (§3.4 / §4.4 CDC).
     */
    public Ticket enregistrerSortie(String numeroTicket) {
        return enregistrerSortie(numeroTicket, LocalDateTime.now());
    }

    /**
     * Enregistre une sortie par numéro de ticket à une date donnée.
     */
    public Ticket enregistrerSortie(String numeroTicket, LocalDateTime dateSortie) {
        if (numeroTicket == null || numeroTicket.isBlank()) {
            throw new ValidationException("Le numéro de ticket est obligatoire");
        }
        int idUser = requireUtilisateurConnecte();

        return executeInTransaction(conn -> {
            Ticket ticket = ticketDao.findByNumeroTicket(conn, numeroTicket.trim())
                    .orElseThrow(() -> new TicketIntrouvableException(
                            "Aucun ticket trouvé pour : " + numeroTicket.trim()));

            return enregistrerSortie(conn, ticket, dateSortie, idUser);
        });
    }

    /**
     * Enregistre une sortie par numéro de place affiché (§4.4 CDC).
     */
    public Ticket enregistrerSortieParNumeroPlace(int numeroPlace) {
        return enregistrerSortieParNumeroPlace(numeroPlace, LocalDateTime.now());
    }

    public Ticket enregistrerSortieParNumeroPlace(int numeroPlace, LocalDateTime dateSortie) {
        if (numeroPlace <= 0) {
            throw new ValidationException("Numéro de place invalide : " + numeroPlace);
        }
        int idUser = requireUtilisateurConnecte();

        return executeInTransaction(conn -> {
            Place place = placeDao.findByNumeroPlace(conn, numeroPlace)
                    .orElseThrow(() -> new TicketIntrouvableException(
                            "Place introuvable : " + numeroPlace));

            Ticket ticket = ticketDao.findActifByIdPlace(conn, place.getIdPlace())
                    .orElseThrow(() -> new TicketIntrouvableException(
                            "Aucun ticket actif sur la place " + numeroPlace));

            return enregistrerSortie(conn, ticket, dateSortie, idUser);
        });
    }

    /**
     * Calcule le montant dû pour un ticket actif sans enregistrer la sortie (aperçu caisse).
     */
    public BigDecimal calculerMontantSortie(String numeroTicket) {
        return calculerMontantSortie(numeroTicket, LocalDateTime.now());
    }

    public BigDecimal calculerMontantSortie(String numeroTicket, LocalDateTime dateSortie) {
        Ticket ticket = trouverTicketActif(numeroTicket);
        return tarifService.calculerMontant(ticket.getDateEntree(), dateSortie);
    }

    public long calculerDureeMinutes(String numeroTicket) {
        return calculerDureeMinutes(numeroTicket, LocalDateTime.now());
    }

    public long calculerDureeMinutes(String numeroTicket, LocalDateTime dateSortie) {
        Ticket ticket = trouverTicketActif(numeroTicket);
        return tarifService.calculerDureeMinutes(ticket.getDateEntree(), dateSortie);
    }

    private Ticket enregistrerEntree(Connection connection, String immatriculation,
                                     int idUser, LocalDateTime dateEntree) throws SQLException {
        Place place = placeDao.findFirstLibreForUpdate(connection)
                .orElseThrow(() -> new ParkingCompletException(
                        "Parking complet — aucune place libre disponible"));

        if (!placeDao.occuper(connection, place.getIdPlace())) {
            throw new PlaceDejaOccupeeException(
                    "La place n°" + place.getNumeroPlace() + " vient d'être occupée par un autre véhicule");
        }

        String numeroTicket = sequenceTicketDao.nextNumeroTicket(connection);

        Ticket ticket = new Ticket();
        ticket.setNumeroTicket(numeroTicket);
        ticket.setIdPlace(place.getIdPlace());
        ticket.setImmatriculation(immatriculation);
        ticket.setDateEntree(dateEntree);
        ticket.setStatut(StatutTicket.ACTIF);

        ticketDao.insert(connection, ticket);

        logDao.insert(connection, new Log(idUser, "ENTREE",
                String.format("Ticket %s — place n°%d — immat %s",
                        numeroTicket, place.getNumeroPlace(), immatriculation)));

        logger.info("Entrée enregistrée — {} place n°{}", numeroTicket, place.getNumeroPlace());
        return ticket;
    }

    private Ticket enregistrerSortie(Connection connection, Ticket ticket,
                                     LocalDateTime dateSortie, int idUser) throws SQLException {
        if (!ticket.isActif()) {
            throw new TicketIntrouvableException(
                    "Le ticket " + ticket.getNumeroTicket() + " n'est plus actif");
        }

        BigDecimal montant = tarifService.calculerMontant(ticket.getDateEntree(), dateSortie);

        if (!ticketDao.updateForSortie(connection, ticket.getIdTicket(), dateSortie, montant)) {
            throw new TicketIntrouvableException(
                    "Impossible d'archiver le ticket " + ticket.getNumeroTicket() + " (déjà traité)");
        }

        if (!placeDao.liberer(connection, ticket.getIdPlace())) {
            throw new PlaceDejaOccupeeException(
                    "Impossible de libérer la place associée au ticket " + ticket.getNumeroTicket());
        }

        ticket.setDateSortie(dateSortie);
        ticket.setMontant(montant);
        ticket.setStatut(StatutTicket.PAYE);

        logDao.insert(connection, new Log(idUser, "SORTIE",
                String.format("Ticket %s — montant %.2f €", ticket.getNumeroTicket(), montant)));

        logger.info("Sortie enregistrée — {} montant {} €", ticket.getNumeroTicket(), montant);
        return ticket;
    }

    private Ticket trouverTicketActif(String numeroTicket) {
        if (numeroTicket == null || numeroTicket.isBlank()) {
            throw new ValidationException("Le numéro de ticket est obligatoire");
        }
        return ticketDao.findByNumeroTicket(numeroTicket.trim())
                .filter(Ticket::isActif)
                .orElseThrow(() -> new TicketIntrouvableException(
                        "Aucun ticket actif pour : " + numeroTicket.trim()));
    }

    private int requireUtilisateurConnecte() {
        return sessionManager.getCurrentUser()
                .map(u -> {
                    sessionManager.touch();
                    return u.getIdUser();
                })
                .orElseThrow(() -> new IllegalStateException(
                        "Authentification requise pour cette opération"));
    }

    static String validerEtNormaliserImmatriculation(String immatriculation) {
        if (immatriculation == null || immatriculation.isBlank()) {
            throw new ValidationException("L'immatriculation est obligatoire");
        }
        String normalisee = immatriculation.trim().toUpperCase().replaceAll("\\s+", " ");
        if (!IMMATRICULATION_PATTERN.matcher(normalisee).matches()) {
            throw new ValidationException(
                    "Format d'immatriculation invalide (2 à 20 caractères alphanumériques, espaces ou tirets)");
        }
        return normalisee;
    }
}
