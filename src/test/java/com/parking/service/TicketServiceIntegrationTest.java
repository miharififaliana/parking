package com.parking.service;

import com.parking.dao.AbstractDaoIntegrationTest;
import com.parking.dao.LogDao;
import com.parking.dao.PlaceDao;
import com.parking.dao.SequenceTicketDao;
import com.parking.dao.TicketDao;
import com.parking.exception.TicketIntrouvableException;
import com.parking.exception.ValidationException;
import com.parking.model.Place;
import com.parking.model.StatutPlace;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration du cycle entrée/sortie via {@link TicketService}.
 */
class TicketServiceIntegrationTest extends AbstractDaoIntegrationTest {

    private AuthService authService;
    private TicketService ticketService;
    private PlaceDao placeDao;

    @BeforeEach
    void setUp() {
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.endSession();

        placeDao = new PlaceDao(databaseConfig);
        LogDao logDao = new LogDao(databaseConfig);

        authService = new AuthService(
                databaseConfig,
                new com.parking.dao.UtilisateurDao(databaseConfig),
                logDao,
                sessionManager
        );
        ticketService = new TicketService(
                databaseConfig,
                placeDao,
                new TicketDao(databaseConfig),
                new SequenceTicketDao(databaseConfig),
                logDao,
                new TarifService(),
                sessionManager
        );

        authService.login("admin", "Admin1234!");
    }

    @AfterEach
    void tearDown() {
        authService.logout();
    }

    @Test
    void proposerPlaceLibre_retourneLaPlusPetitePlaceLibre() {
        Place place = ticketService.proposerPlaceLibre().orElseThrow();

        assertTrue(place.getNumeroPlace() >= 1);
        assertEquals(StatutPlace.LIBRE, place.getStatut());
    }

    @Test
    void enregistrerEntree_etSortie_cycleComplet() {
        Ticket entree = ticketService.enregistrerEntree("1234-TAA");

        assertNotNull(entree.getNumeroTicket());
        assertTrue(entree.getNumeroTicket().startsWith("TKT-"));
        assertEquals("1234-TAA", entree.getImmatriculation());
        assertEquals(StatutTicket.ACTIF, entree.getStatut());

        Place placeOccupee = placeDao.findById(entree.getIdPlace()).orElseThrow();
        assertEquals(StatutPlace.OCCUPEE, placeOccupee.getStatut());

        BigDecimal montantPreview = ticketService.calculerMontantSortie(entree.getNumeroTicket());
        assertEquals(new BigDecimal("1.00"), montantPreview);

        Ticket sortie = ticketService.enregistrerSortie(entree.getNumeroTicket());

        assertEquals(StatutTicket.PAYE, sortie.getStatut());
        assertEquals(new BigDecimal("1.00"), sortie.getMontant());
        assertNotNull(sortie.getDateSortie());

        Place placeLiberee = placeDao.findById(entree.getIdPlace()).orElseThrow();
        assertEquals(StatutPlace.LIBRE, placeLiberee.getStatut());
    }

    @Test
    void enregistrerSortieParNumeroPlace_trouveLeTicketActif() {
        Ticket entree = ticketService.enregistrerEntree("AB-123-CD");
        Place place = placeDao.findById(entree.getIdPlace()).orElseThrow();

        Ticket sortie = ticketService.enregistrerSortieParNumeroPlace(place.getNumeroPlace());

        assertEquals(entree.getNumeroTicket(), sortie.getNumeroTicket());
        assertEquals(StatutTicket.PAYE, sortie.getStatut());
    }

    @Test
    void enregistrerEntree_rejetteImmatriculationVide() {
        assertThrows(ValidationException.class, () -> ticketService.enregistrerEntree("  "));
    }

    @Test
    void enregistrerEntree_requiertAuthentification() {
        authService.logout();

        assertThrows(IllegalStateException.class, () -> ticketService.enregistrerEntree("1234-TAA"));
    }

    @Test
    void enregistrerSortie_rejetteTicketInconnu() {
        assertThrows(TicketIntrouvableException.class,
                () -> ticketService.enregistrerSortie("TKT-2099-9999"));
    }

    @Test
    void validerImmatriculation_normaliseEtMajuscules() {
        assertEquals("1234 TAA", TicketService.validerEtNormaliserImmatriculation("  1234  taa "));
    }
}
