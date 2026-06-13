package com.parking.config;

import com.parking.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Singleton de configuration de l'application.
 *
 * <p>Stratégie de chargement (par ordre de priorité) :</p>
 * <ol>
 *   <li>Fichier {@code config.properties} dans le répertoire courant
 *       (déploiement production — hors JAR, §5.4 CDC)</li>
 *   <li>Fichier {@code config.properties} dans le classpath
 *       (développement / tests)</li>
 * </ol>
 *
 * <p>Thread-safe grâce à l'initialisation à la demande (initialization-on-demand
 * holder idiom).</p>
 */
public final class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /** Nom du fichier de configuration */
    private static final String CONFIG_FILE = "config.properties";

    /** Propriétés chargées */
    private final Properties props;

    // ── Constructeur privé (Singleton) ────────────────────────────────────────

    private AppConfig() {
        this.props = new Properties();
        loadProperties();
    }

    // ── Holder pattern (thread-safe, lazy init) ───────────────────────────────

    private static final class Holder {
        private static final AppConfig INSTANCE = new AppConfig();
    }

    /**
     * Retourne l'unique instance de {@code AppConfig}.
     *
     * @return l'instance singleton
     * @throws ConfigurationException si le fichier de configuration est introuvable ou illisible
     */
    public static AppConfig getInstance() {
        return Holder.INSTANCE;
    }

    // ── Chargement des propriétés ─────────────────────────────────────────────

    /**
     * Charge le fichier config.properties.
     * Priorité : répertoire courant (production) → classpath (dev/tests).
     */
    private void loadProperties() {
        // Tentative 1 : fichier externe dans le répertoire courant (production)
        Path externalConfig = Paths.get(CONFIG_FILE);
        if (Files.exists(externalConfig)) {
            try (InputStream is = Files.newInputStream(externalConfig)) {
                props.load(is);
                logger.info("Configuration chargée depuis le fichier externe : {}",
                        externalConfig.toAbsolutePath());
                return;
            } catch (IOException e) {
                logger.warn("Impossible de lire le fichier externe {}, fallback classpath : {}",
                        externalConfig, e.getMessage());
            }
        }

        // Tentative 2 : classpath (développement / tests)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new ConfigurationException(
                        "Fichier config.properties introuvable (ni en externe, ni dans le classpath). "
                                + "Copiez config.properties.template vers config.properties et remplissez les valeurs.");
            }
            props.load(is);
            logger.info("Configuration chargée depuis le classpath (mode développement)");
        } catch (IOException e) {
            throw new ConfigurationException("Erreur de lecture de config.properties : " + e.getMessage(), e);
        }
    }

    // ── Accesseurs typés ──────────────────────────────────────────────────────

    /**
     * Retourne une propriété String, obligatoire.
     *
     * @param key     clé de propriété
     * @return la valeur
     * @throws ConfigurationException si la clé est absente ou vide
     */
    private String getRequired(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Propriété obligatoire manquante dans config.properties : " + key);
        }
        return value.trim();
    }

    /**
     * Retourne une propriété String avec valeur par défaut.
     */
    private String get(String key, String defaultValue) {
        String value = props.getProperty(key, defaultValue);
        return (value == null) ? defaultValue : value.trim();
    }

    /**
     * Retourne une propriété entière avec valeur par défaut.
     */
    private int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Valeur invalide pour '{}', utilisation du défaut : {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Retourne une propriété booléenne avec valeur par défaut.
     */
    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    // ── API publique : Base de données ────────────────────────────────────────

    public String getDbHost()     { return get("db.host", "localhost"); }
    public int    getDbPort()     { return getInt("db.port", 3306); }
    public String getDbName()     { return getRequired("db.name"); }
    public String getDbUser()     { return getRequired("db.user"); }

    /**
     * Retourne le mot de passe de la base de données.
     * Une chaîne vide est acceptée (environnement local de développement).
     * <strong>Ne jamais logger cette valeur.</strong>
     */
    public String getDbPassword() { return get("db.password", ""); }

    public int getDbPoolMin()            { return getInt("db.pool.minConnections", 2); }
    public int getDbPoolMax()            { return getInt("db.pool.maxConnections", 10); }
    public int getDbConnectionTimeout()  { return getInt("db.pool.connectionTimeout", 30000); }

    /**
     * Construit l'URL JDBC à partir des paramètres de configuration.
     * Paramètres inclus : encodage UTF-8, timezone, reconnexion automatique.
     */
    public String getJdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8"
                        + "&serverTimezone=Europe/Paris&autoReconnect=true&useSSL=false",
                getDbHost(), getDbPort(), getDbName()
        );
    }

    // ── API publique : Tarification ───────────────────────────────────────────

    public BigDecimal getTarifHoraire() {
        try {
            return new BigDecimal(get("tarif.horaire", "1.00"));
        } catch (NumberFormatException e) {
            logger.warn("tarif.horaire invalide, valeur par défaut 1.00 utilisée");
            return BigDecimal.ONE;
        }
    }

    public int getDureeMinimumMinutes() { return getInt("tarif.dureeMinimumMinutes", 60); }

    // ── API publique : Parking ────────────────────────────────────────────────

    public String getParkingNom()      { return get("parking.nom", "Parking Moto"); }
    public String getParkingAdresse()  { return get("parking.adresse", ""); }
    public String getParkingTelephone(){ return get("parking.telephone", ""); }
    public int    getNombrePlaces()    { return getInt("parking.nombrePlaces", 50); }

    // ── API publique : Tickets ────────────────────────────────────────────────

    public String getTicketPrefixe()      { return get("ticket.prefixe", "TKT"); }
    public int    getTicketSequenceLength(){ return getInt("ticket.sequenceLength", 4); }

    // ── API publique : Impression ─────────────────────────────────────────────

    public String  getImpressionPort()   { return get("impression.port", ""); }
    public int     getImpressionLargeur(){ return getInt("impression.largeur", 32); }
    public boolean isImpressionActive()  { return getBoolean("impression.active", false); }

    // ── API publique : Interface utilisateur ──────────────────────────────────

    public double getUiLargeur()            { return getInt("ui.largeur", 1280); }
    public double getUiHauteur()            { return getInt("ui.hauteur", 720); }
    public String getUiTitre()              { return get("ui.titre", "Parking Moto"); }
    public int    getDashboardRefreshMs()   { return getInt("ui.dashboard.refreshInterval", 5000); }

    // ── API publique : Sécurité ───────────────────────────────────────────────

    public int getSecurityBcryptCost()   { return getInt("security.bcryptCost", 12); }
    public int getSecuritySessionTimeout(){ return getInt("security.sessionTimeout", 480); }

    // ── API publique : Sauvegarde ─────────────────────────────────────────────

    public String getBackupRepertoire() { return get("backup.repertoire", "./backups"); }
    public int    getBackupRetention()  { return getInt("backup.retention", 30); }
}