package com.parking.dao;

import com.parking.model.Log;
import com.parking.model.Place;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration pour {@link SequenceTicketDao}, {@link TicketDao} et {@link LogDao}.
 */
class TicketFlowDaoIntegrationTest extends AbstractDaoIntegrationTest {

    private final PlaceDao placeDao = new PlaceDao(databaseConfig);
    private final TicketDao ticketDao = new TicketDao(databaseConfig);
    private final SequenceTicketDao sequenceTicketDao = new SequenceTicketDao(databaseConfig);
    private final LogDao logDao = new LogDao(databaseConfig);
    private final UtilisateurDao utilisateurDao = new UtilisateurDao(databaseConfig);

    @Test
    void nextNumeroTicket_respecteLeFormatTktAnneeSequence() throws Exception {
        executeThenRollback(connection -> {
            String numero = sequenceTicketDao.nextNumeroTicket(connection);
            int annee = Year.now().getValue();
            assertTrue(numero.matches("TKT-" + annee + "-\\d{4}"));
        });
    }

    @Test
    void nextSequence_incrementeSansDoublonDansUneTransaction() throws Exception {
        executeThenRollback(connection -> {
            int annee = Year.now().getValue();
            int first = sequenceTicketDao.nextSequence(connection, annee);
            int second = sequenceTicketDao.nextSequence(connection, annee);
            assertEquals(first + 1, second);
        });
    }

    @Test
    void insertEtUpdateForSortie_gereLeCycleEntreeSortie() throws Exception {
        executeThenRollback(connection -> {
            Place place = placeDao.findFirstLibreForUpdate(connection)
                    .orElseThrow(() -> new AssertionError("Aucune place libre"));

            assertTrue(placeDao.occuper(connection, place.getIdPlace()));

            String numero = sequenceTicketDao.nextNumeroTicket(connection);
            Ticket ticket = new Ticket();
            ticket.setNumeroTicket(numero);
            ticket.setIdPlace(place.getIdPlace());
            ticket.setImmatriculation("1234-TAA");
            ticket.setDateEntree(LocalDateTime.now());
            ticket.setStatut(StatutTicket.ACTIF);

            int idTicket = ticketDao.insert(connection, ticket);
            assertTrue(idTicket > 0);

            Optional<Ticket> actif = ticketDao.findActifByIdPlace(connection, place.getIdPlace());
            assertTrue(actif.isPresent());
            assertEquals(numero, actif.get().getNumeroTicket());

            assertTrue(ticketDao.updateForSortie(connection, idTicket, LocalDateTime.now(), new BigDecimal("2.00")));
            assertFalse(ticketDao.updateForSortie(connection, idTicket, LocalDateTime.now(), BigDecimal.ONE));

            assertTrue(placeDao.liberer(connection, place.getIdPlace()));
        });
    }

    @Test
    void findByNumeroTicket_retourneLeTicketInsere() throws Exception {
        executeThenRollback(connection -> {
            Place place = placeDao.findFirstLibreForUpdate(connection)
                    .orElseThrow(() -> new AssertionError("Aucune place libre"));
            placeDao.occuper(connection, place.getIdPlace());

            String numero = sequenceTicketDao.nextNumeroTicket(connection);
            Ticket ticket = new Ticket();
            ticket.setNumeroTicket(numero);
            ticket.setIdPlace(place.getIdPlace());
            ticket.setImmatriculation("AB-123-CD");
            ticket.setDateEntree(LocalDateTime.now());
            ticket.setStatut(StatutTicket.ACTIF);
            ticketDao.insert(connection, ticket);

            Optional<Ticket> trouve = ticketDao.findByNumeroTicket(connection, numero);
            assertTrue(trouve.isPresent());
            assertEquals("AB-123-CD", trouve.get().getImmatriculation());
        });
    }

    @Test
    void logDao_insertEtRecherche() throws Exception {
        executeThenRollback(connection -> {
            int idUser = utilisateurDao.findByLogin(connection, "admin")
                    .map(u -> u.getIdUser())
                    .orElseThrow(() -> new AssertionError("Compte admin absent"));

            Log log = new Log(idUser, "TEST_DAO", "Test d'intégration étape 2");
            int idLog = logDao.insert(connection, log);
            assertTrue(idLog > 0);

            List<Log> derniers = logDao.findDerniers(connection, 5);
            assertFalse(derniers.isEmpty());
            assertEquals("TEST_DAO", derniers.get(0).getAction());
        });
    }
}
