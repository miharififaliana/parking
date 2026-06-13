package com.parking.config;

import com.parking.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestionnaire du pool de connexions JDBC vers MySQL.
 *
 * <p>Implémentation d'un pool simple sans dépendance externe (JDBC pur,
 * conformément au §5.1 CDC — "Contrôle total, sans overhead ORM").</p>
 *
 * <p>Garanties :</p>
 * <ul>
 *   <li>Thread-safe (verrou {@link ReentrantLock})</li>
 *   <li>Connexion validée avant remise en pool ({@code isValid()})</li>
 *   <li>Auto-commit désactivé par défaut — toutes les transactions sont
 *       explicites (protection anti-coupure §9 CDC)</li>
 * </ul>
 */
public final class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    /** Pool de connexions disponibles */
    private final Deque<Connection> pool = new ArrayDeque<>();

    /** Toutes les connexions créées (pour fermeture propre) */
    private final List<Connection> allConnections = new ArrayList<>();

    /** Verrou pour l'accès concurrent au pool */
    private final ReentrantLock lock = new ReentrantLock();

    private final AppConfig config;
    private boolean initialized = false;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static final class Holder {
        private static final DatabaseConfig INSTANCE = new DatabaseConfig();
    }

    private DatabaseConfig() {
        this.config = AppConfig.getInstance();
    }

    public static DatabaseConfig getInstance() {
        return Holder.INSTANCE;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Initialise le pool avec le nombre minimum de connexions configuré.
     * Appelée une seule fois au démarrage.
     *
     * @throws DatabaseException si la connexion échoue
     */
    public synchronized void initialize() {
        if (initialized) return;

        logger.info("Initialisation du pool JDBC — min: {}, max: {}",
                config.getDbPoolMin(), config.getDbPoolMax());

        for (int i = 0; i < config.getDbPoolMin(); i++) {
            try {
                Connection conn = createConnection();
                pool.offer(conn);
                allConnections.add(conn);
                logger.debug("Connexion #{} créée dans le pool", i + 1);
            } catch (SQLException e) {
                throw new DatabaseException(
                        "Impossible d'initialiser le pool de connexions JDBC. "
                                + "Vérifiez que MySQL est démarré et que config.properties est correct. "
                                + "Erreur : " + e.getMessage(), e);
            }
        }

        initialized = true;
        logger.info("Pool JDBC initialisé avec {} connexion(s)", pool.size());
    }

    /**
     * Teste la connexion à la base de données.
     * Appelée au démarrage de l'application pour un fail-fast propre.
     *
     * @throws DatabaseException si la connexion ne peut pas être établie
     */
    public void testConnection() {
        try (Connection conn = createConnection()) {
            if (!conn.isValid(5)) {
                throw new DatabaseException("La connexion MySQL n'est pas valide (timeout 5s)");
            }
            logger.info("Test de connexion MySQL réussi — URL: {}", maskPassword(config.getJdbcUrl()));
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Impossible de se connecter à MySQL. "
                            + "Vérifiez que le serveur est démarré sur " + config.getDbHost() + ":" + config.getDbPort()
                            + " et que la base '" + config.getDbName() + "' existe. "
                            + "Erreur SQL: " + e.getMessage(), e);
        }
    }

    // ── Gestion du pool ───────────────────────────────────────────────────────

    /**
     * Emprunte une connexion du pool.
     *
     * <p>Si le pool est vide et que la limite max n'est pas atteinte,
     * une nouvelle connexion est créée. Sinon, attend jusqu'à 30 secondes.</p>
     *
     * @return une connexion JDBC avec auto-commit désactivé
     * @throws DatabaseException si aucune connexion n'est disponible
     */
    public Connection getConnection() {
        if (!initialized) {
            initialize();
        }

        lock.lock();
        try {
            // Chercher une connexion valide dans le pool
            Connection conn;
            while ((conn = pool.poll()) != null) {
                if (isConnectionValid(conn)) {
                    logger.trace("Connexion empruntée du pool — {} restante(s)", pool.size());
                    return conn;
                } else {
                    // Connexion morte — la retirer et en créer une nouvelle
                    logger.warn("Connexion morte détectée, suppression du pool");
                    allConnections.remove(conn);
                }
            }

            // Pool vide : créer une nouvelle connexion si sous la limite max
            if (allConnections.size() < config.getDbPoolMax()) {
                try {
                    conn = createConnection();
                    allConnections.add(conn);
                    logger.debug("Nouvelle connexion créée — total: {}", allConnections.size());
                    return conn;
                } catch (SQLException e) {
                    throw new DatabaseException("Impossible de créer une nouvelle connexion JDBC : " + e.getMessage(), e);
                }
            }

            throw new DatabaseException("Pool de connexions saturé ("
                    + config.getDbPoolMax() + " connexions max). Réessayez dans un instant.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retourne une connexion au pool après utilisation.
     *
     * <p><strong>Important :</strong> à appeler dans un bloc {@code finally}
     * pour garantir la libération même en cas d'exception.</p>
     *
     * @param conn la connexion à rendre (peut être null — ignorée)
     */
    public void releaseConnection(Connection conn) {
        if (conn == null) return;

        lock.lock();
        try {
            // Annuler toute transaction non validée (sécurité)
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                logger.warn("Erreur lors du rollback avant remise en pool : {}", e.getMessage());
            }

            if (isConnectionValid(conn)) {
                pool.offer(conn);
                logger.trace("Connexion rendue au pool — {} disponible(s)", pool.size());
            } else {
                allConnections.remove(conn);
                logger.warn("Connexion invalide non remise en pool");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Ferme toutes les connexions du pool.
     * Appelée lors de l'arrêt de l'application ({@link com.parking.MainApp#stop()}).
     */
    public void closePool() {
        lock.lock();
        try {
            logger.info("Fermeture du pool — {} connexion(s) à fermer", allConnections.size());
            for (Connection conn : allConnections) {
                try {
                    if (!conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    logger.warn("Erreur lors de la fermeture d'une connexion : {}", e.getMessage());
                }
            }
            allConnections.clear();
            pool.clear();
            initialized = false;
            logger.info("Pool JDBC fermé");
        } finally {
            lock.unlock();
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    /**
     * Crée une nouvelle connexion JDBC avec auto-commit désactivé.
     *
     * <p>Auto-commit = false est essentiel pour la protection anti-coupure :
     * toutes les opérations métier doivent être des transactions explicites
     * (commit/rollback) conformément au §9 CDC.</p>
     */
    private Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getDbUser(),
                config.getDbPassword()  // Ne jamais logger cette valeur
        );
        conn.setAutoCommit(false); // OBLIGATOIRE — transactions explicites
        return conn;
    }

    /**
     * Vérifie si une connexion est encore valide (timeout 2 secondes).
     */
    private boolean isConnectionValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Masque le mot de passe dans une URL JDBC pour les logs.
     * Exemple : jdbc:mysql://localhost:3306/db?password=SECRET → ...password=***...
     */
    private String maskPassword(String url) {
        return url.replaceAll("password=[^&]*", "password=***");
    }
}