package com.parking.service;

import com.parking.config.DatabaseConfig;
import com.parking.dao.LogDao;
import com.parking.dao.UtilisateurDao;
import com.parking.model.Log;
import com.parking.model.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service d'authentification (§4.1 CDC — BCrypt + session utilisateur).
 */
public class AuthService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UtilisateurDao utilisateurDao;
    private final LogDao logDao;
    private final SessionManager sessionManager;

    public AuthService() {
        this(new UtilisateurDao(), new LogDao(), SessionManager.getInstance());
    }

    AuthService(UtilisateurDao utilisateurDao, LogDao logDao, SessionManager sessionManager) {
        super();
        this.utilisateurDao = utilisateurDao;
        this.logDao = logDao;
        this.sessionManager = sessionManager;
    }

    AuthService(DatabaseConfig databaseConfig, UtilisateurDao utilisateurDao,
                LogDao logDao, SessionManager sessionManager) {
        super(databaseConfig);
        this.utilisateurDao = utilisateurDao;
        this.logDao = logDao;
        this.sessionManager = sessionManager;
    }

    /**
     * Authentifie un utilisateur et ouvre une session applicative.
     *
     * @return l'utilisateur connecté, ou vide si identifiants invalides
     */
    public Optional<Utilisateur> login(String login, String password) {
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        Optional<Utilisateur> utilisateur = utilisateurDao.authenticate(login, password);
        if (utilisateur.isEmpty()) {
            logger.warn("Échec de connexion pour le login : {}", login.trim());
            return Optional.empty();
        }

        Utilisateur user = utilisateur.get();
        sessionManager.startSession(user);

        logDao.insert(new Log(user.getIdUser(), "LOGIN",
                "Connexion réussie — rôle " + user.getRole()));

        logger.info("Utilisateur connecté : {} ({})", user.getLogin(), user.getRole());
        return Optional.of(user);
    }

    /**
     * Ferme la session courante et journalise la déconnexion.
     */
    public void logout() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            logDao.insert(new Log(user.getIdUser(), "LOGOUT", "Déconnexion manuelle"));
            logger.info("Utilisateur déconnecté : {}", user.getLogin());
            sessionManager.endSession();
        });
    }

    /**
     * Retourne l'utilisateur connecté (session valide).
     */
    public Optional<Utilisateur> getUtilisateurConnecte() {
        return sessionManager.getCurrentUser();
    }

    public boolean isAuthenticated() {
        return sessionManager.isLoggedIn();
    }
}
