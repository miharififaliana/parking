package com.parking.service;

import com.parking.model.Utilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires du {@link SessionManager}.
 */
class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.endSession();
    }

    @AfterEach
    void tearDown() {
        sessionManager.endSession();
    }

    @Test
    void startSession_etGetCurrentUser() {
        Utilisateur user = new Utilisateur();
        user.setIdUser(1);
        user.setLogin("gerant");

        sessionManager.startSession(user);

        Optional<Utilisateur> current = sessionManager.getCurrentUser();
        assertTrue(current.isPresent());
        assertEqualsLogin("gerant", current.get());
    }

    @Test
    void endSession_invalideLaSession() {
        Utilisateur user = new Utilisateur();
        user.setIdUser(1);
        user.setLogin("gerant");
        sessionManager.startSession(user);

        sessionManager.endSession();

        assertFalse(sessionManager.isLoggedIn());
        assertFalse(sessionManager.getCurrentUser().isPresent());
    }

    @Test
    void touch_prolongeLaSessionActive() {
        Utilisateur user = new Utilisateur();
        user.setIdUser(1);
        user.setLogin("gerant");
        sessionManager.startSession(user);

        sessionManager.touch();

        assertTrue(sessionManager.isLoggedIn());
    }

    private static void assertEqualsLogin(String expected, Utilisateur actual) {
        assertEquals(expected, actual.getLogin());
    }
}
