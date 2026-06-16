package com.parking.dao;

import com.parking.model.Role;
import com.parking.model.Utilisateur;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration pour {@link UtilisateurDao}.
 */
class UtilisateurDaoIntegrationTest extends AbstractDaoIntegrationTest {

    private final UtilisateurDao utilisateurDao = new UtilisateurDao(databaseConfig);

    @Test
    void findByLogin_retourneLeCompteAdmin() {
        Optional<Utilisateur> admin = utilisateurDao.findByLogin("admin");

        assertTrue(admin.isPresent());
        assertTrue(admin.get().isActif());
        assertEquals(Role.ADMIN, admin.get().getRole());
    }

    @Test
    void authenticate_valideAdmin1234() {
        Optional<Utilisateur> authentifie = utilisateurDao.authenticate("admin", "Admin1234!");

        assertTrue(authentifie.isPresent());
        assertEquals(Role.ADMIN, authentifie.get().getRole());
    }

    @Test
    void authenticate_rejetteMotDePasseIncorrect() {
        Optional<Utilisateur> authentifie = utilisateurDao.authenticate("admin", "mauvais");

        assertFalse(authentifie.isPresent());
    }

    @Test
    void authenticate_rejetteLoginInconnu() {
        Optional<Utilisateur> authentifie = utilisateurDao.authenticate("inexistant", "Admin1234!");

        assertFalse(authentifie.isPresent());
    }

    @Test
    void verifyPassword_valideUnHashGenere() {
        String hash = utilisateurDao.hashPassword("TestSecret42!");
        assertTrue(utilisateurDao.verifyPassword("TestSecret42!", hash));
        assertFalse(utilisateurDao.verifyPassword("autre", hash));
    }
}
