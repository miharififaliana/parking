package com.parking.dao;

import com.parking.config.AppConfig;
import com.parking.config.DatabaseConfig;
import com.parking.model.Utilisateur;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Accès JDBC à la table {@code utilisateurs} (§4.1 CDC — authentification BCrypt).
 */
public class UtilisateurDao extends BaseDao {

    private static final String SELECT_BY_LOGIN = """
            SELECT id_user, nom, login, mot_de_passe, role, actif, created_at, updated_at
            FROM utilisateurs
            WHERE login = ?
            """;

    public UtilisateurDao() {
        super();
    }

    UtilisateurDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    /**
     * Recherche un utilisateur par son identifiant de connexion.
     */
    public Optional<Utilisateur> findByLogin(String login) {
        return query(conn -> findByLogin(conn, login));
    }

    /**
     * Recherche un utilisateur par login dans une transaction existante.
     */
    public Optional<Utilisateur> findByLogin(Connection connection, String login) throws SQLException {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_LOGIN)) {
            ps.setString(1, login.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapUtilisateur(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Vérifie un mot de passe en clair contre un hash BCrypt stocké en base.
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    /**
     * Authentifie un utilisateur actif par login / mot de passe.
     *
     * @return l'utilisateur si les identifiants sont valides et le compte actif
     */
    public Optional<Utilisateur> authenticate(String login, String plainPassword) {
        return findByLogin(login)
                .filter(Utilisateur::isActif)
                .filter(user -> verifyPassword(plainPassword, user.getMotDePasse()));
    }

    /**
     * Hache un mot de passe avec le facteur BCrypt configuré (≥ 12, §5.4 CDC).
     */
    public String hashPassword(String plainPassword) {
        int cost = AppConfig.getInstance().getSecurityBcryptCost();
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(cost));
    }
}
