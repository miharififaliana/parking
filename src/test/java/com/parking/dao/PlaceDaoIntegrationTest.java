package com.parking.dao;

import com.parking.model.Place;
import com.parking.model.StatutPlace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration pour {@link PlaceDao}.
 */
class PlaceDaoIntegrationTest extends AbstractDaoIntegrationTest {

    private final PlaceDao placeDao = new PlaceDao(databaseConfig);

    @Test
    void findAll_retourneLesPlacesTrieesParNumero() {
        List<Place> places = placeDao.findAll();

        assertFalse(places.isEmpty());
        assertTrue(places.size() >= 50);
        assertEquals(1, places.get(0).getNumeroPlace());
        for (int i = 1; i < places.size(); i++) {
            assertTrue(places.get(i - 1).getNumeroPlace() <= places.get(i).getNumeroPlace());
        }
    }

    @Test
    void findFirstLibre_retourneLaPlusPetitePlaceLibre() throws Exception {
        executeThenRollback(connection -> {
            Optional<Place> first = placeDao.findFirstLibre(connection);
            assertTrue(first.isPresent());
            assertEquals(StatutPlace.LIBRE, first.get().getStatut());

            int libres = placeDao.countByStatut(connection, StatutPlace.LIBRE);
            assertTrue(libres > 0);
        });
    }

    @Test
    void occuperEtLiberer_respecteLeVerrouillageOptimiste() throws Exception {
        executeThenRollback(connection -> {
            Place place = placeDao.findFirstLibreForUpdate(connection)
                    .orElseThrow(() -> new AssertionError("Aucune place libre"));

            assertTrue(placeDao.occuper(connection, place.getIdPlace()));
            assertFalse(placeDao.occuper(connection, place.getIdPlace()));

            Optional<Place> occupee = placeDao.findById(connection, place.getIdPlace());
            assertTrue(occupee.isPresent());
            assertEquals(StatutPlace.OCCUPEE, occupee.get().getStatut());

            assertTrue(placeDao.liberer(connection, place.getIdPlace()));
            assertFalse(placeDao.liberer(connection, place.getIdPlace()));
        });
    }

    @Test
    void countByStatut_sommeLibresEtOccupees() {
        int libres = placeDao.countByStatut(StatutPlace.LIBRE);
        int occupees = placeDao.countByStatut(StatutPlace.OCCUPEE);
        int total = placeDao.findAll().size();

        assertTrue(libres >= 0);
        assertTrue(occupees >= 0);
        assertEquals(total, libres + occupees);
    }

    @Test
    void findById_retourneUnePlaceExistante() {
        Place premiere = placeDao.findAll().get(0);
        Optional<Place> trouvee = placeDao.findById(premiere.getIdPlace());

        assertTrue(trouvee.isPresent());
        assertNotNull(trouvee.get().getStatut());
        assertEquals(premiere.getNumeroPlace(), trouvee.get().getNumeroPlace());
    }
}
