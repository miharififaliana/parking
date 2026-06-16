package com.parking.service;

import com.parking.dao.AbstractDaoIntegrationTest;
import com.parking.model.Role;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;
import com.parking.model.Utilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration pour {@link AuthService}.
 */
class AuthServiceIntegrationTest extends AbstractDaoIntegrationTest {

    private AuthService authService;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.endSession();
        authService = new AuthService(
                databaseConfig,
                new com.parking.dao.UtilisateurDao(databaseConfig),
                new com.parking.dao.LogDao(databaseConfig),
                sessionManager
        );
    }

    @AfterEach
    void tearDown() {
        authService.logout();
    }

    @Test
    void login_ouvreUneSessionAvecAdmin() {
        Optional<Utilisateur> user = authService.login("admin", "Admin1234!");

        assertTrue(user.isPresent());
        assertEquals(Role.ADMIN, user.get().getRole());
        assertTrue(authService.isAuthenticated());
        assertTrue(sessionManager.getCurrentUser().isPresent());
    }

    @Test
    void login_rejetteMotDePasseIncorrect() {
        Optional<Utilisateur> user = authService.login("admin", "wrong");

        assertFalse(user.isPresent());
        assertFalse(authService.isAuthenticated());
    }

    @Test
    void logout_fermeLaSession() {
        authService.login("admin", "Admin1234!");
        authService.logout();

        assertFalse(authService.isAuthenticated());
        assertFalse(sessionManager.getCurrentUser().isPresent());
    }
}
