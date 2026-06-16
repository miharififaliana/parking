package com.parking.service;

import com.parking.dao.AbstractDaoIntegrationTest;
import com.parking.dao.PlaceDao;
import com.parking.dao.TicketDao;
import com.parking.model.StatutPlace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration pour {@link DashboardService}.
 */
class DashboardServiceIntegrationTest extends AbstractDaoIntegrationTest {

    private final DashboardService dashboardService = new DashboardService(
            new PlaceDao(databaseConfig),
            new TicketDao(databaseConfig)
    );

    @Test
    void getSnapshot_retourneLesIndicateursOccupation() {
        var snapshot = dashboardService.getSnapshot();

        assertNotNull(snapshot.getHorodatage());
        assertTrue(snapshot.getTotalPlaces() > 0);
        assertEquals(snapshot.getTotalPlaces(),
                snapshot.getPlacesLibres() + snapshot.getPlacesOccupees());
        assertTrue(snapshot.getTauxOccupation() >= 0 && snapshot.getTauxOccupation() <= 100);
    }

    @Test
    void getSnapshot_coherentAvecPlaceDao() {
        int libres = new PlaceDao(databaseConfig).countByStatut(StatutPlace.LIBRE);
        var snapshot = dashboardService.getSnapshot();

        assertEquals(libres, snapshot.getPlacesLibres());
    }
}
