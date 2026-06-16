package com.parking.service;

import com.parking.config.AppConfig;
import com.parking.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gestionnaire de session utilisateur (singleton thread-safe).
 *
 * <p>Durée de validité configurable via {@code security.sessionTimeout} (480 min par défaut, §4.1 CDC).</p>
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Object lock = new Object();

    private Utilisateur currentUser;
    private LocalDateTime lastActivityTime;
    private int timeoutMinutes = AppConfig.getInstance().getSecuritySessionTimeout();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Démarre une session pour l'utilisateur authentifié.
     */
    public void startSession(Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("Utilisateur de session null");
        }
        synchronized (lock) {
            this.currentUser = utilisateur;
            this.lastActivityTime = LocalDateTime.now();
            this.timeoutMinutes = AppConfig.getInstance().getSecuritySessionTimeout();
        }
    }

    /**
     * Termine la session courante.
     */
    public void endSession() {
        synchronized (lock) {
            this.currentUser = null;
            this.lastActivityTime = null;
        }
    }

    /**
     * Retourne l'utilisateur connecté si la session est encore valide.
     */
    public Optional<Utilisateur> getCurrentUser() {
        synchronized (lock) {
            if (currentUser == null || lastActivityTime == null) {
                return Optional.empty();
            }
            if (isExpired()) {
                currentUser = null;
                lastActivityTime = null;
                return Optional.empty();
            }
            return Optional.of(currentUser);
        }
    }

    /**
     * Rafraîchit l'horodatage d'activité (prolonge implicitement la session).
     */
    public void touch() {
        synchronized (lock) {
            if (currentUser != null && !isExpired()) {
                lastActivityTime = LocalDateTime.now();
            }
        }
    }

    public boolean isLoggedIn() {
        return getCurrentUser().isPresent();
    }

    private boolean isExpired() {
        return lastActivityTime.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }
}
