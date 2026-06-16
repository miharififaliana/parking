package com.parking.dao;

import com.parking.config.DatabaseConfig;
import com.parking.model.Place;
import com.parking.model.StatutPlace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Accès JDBC à la table {@code places} (§3.1 CDC — attribution par plus petit numéro LIBRE).
 */
public class PlaceDao extends BaseDao {

    private static final String SELECT_ALL = """
            SELECT id_place, numero_place, statut, created_at, updated_at
            FROM places
            ORDER BY numero_place ASC
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_place, numero_place, statut, created_at, updated_at
            FROM places
            WHERE id_place = ?
            """;

    private static final String SELECT_BY_NUMERO = """
            SELECT id_place, numero_place, statut, created_at, updated_at
            FROM places
            WHERE numero_place = ?
            """;

    private static final String SELECT_FIRST_LIBRE = """
            SELECT id_place, numero_place, statut, created_at, updated_at
            FROM places
            WHERE statut = 'LIBRE'
            ORDER BY numero_place ASC
            LIMIT 1
            """;

    private static final String SELECT_FIRST_LIBRE_FOR_UPDATE = """
            SELECT id_place, numero_place, statut, created_at, updated_at
            FROM places
            WHERE statut = 'LIBRE'
            ORDER BY numero_place ASC
            LIMIT 1
            FOR UPDATE
            """;

    private static final String UPDATE_STATUT_IF_CURRENT = """
            UPDATE places
            SET statut = ?
            WHERE id_place = ?
              AND statut = ?
            """;

    private static final String COUNT_BY_STATUT = """
            SELECT COUNT(*) AS total
            FROM places
            WHERE statut = ?
            """;

    public PlaceDao() {
        super();
    }

    PlaceDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    /**
     * Retourne toutes les places triées par numéro croissant.
     */
    public List<Place> findAll() {
        return query(this::findAll);
    }

    public List<Place> findAll(Connection connection) throws SQLException {
        List<Place> places = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                places.add(JdbcMapper.mapPlace(rs));
            }
        }
        return places;
    }

    /**
     * Recherche une place par son identifiant technique.
     */
    public Optional<Place> findById(int idPlace) {
        return query(conn -> findById(conn, idPlace));
    }

    public Optional<Place> findById(Connection connection, int idPlace) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, idPlace);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapPlace(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Recherche une place par son numéro affiché (§4.4 CDC — recherche sortie par place).
     */
    public Optional<Place> findByNumeroPlace(int numeroPlace) {
        return query(conn -> findByNumeroPlace(conn, numeroPlace));
    }

    public Optional<Place> findByNumeroPlace(Connection connection, int numeroPlace) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_NUMERO)) {
            ps.setInt(1, numeroPlace);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapPlace(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Retourne la première place libre (plus petit {@code numero_place}), sans verrouillage.
     */
    public Optional<Place> findFirstLibre() {
        return query(this::findFirstLibre);
    }

    public Optional<Place> findFirstLibre(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_FIRST_LIBRE);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(JdbcMapper.mapPlace(rs));
            }
            return Optional.empty();
        }
    }

    /**
     * Retourne la première place libre avec verrouillage pessimiste ({@code FOR UPDATE}).
     * À utiliser dans une transaction d'entrée pour éviter les doubles attributions (§9 CDC).
     */
    public Optional<Place> findFirstLibreForUpdate(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_FIRST_LIBRE_FOR_UPDATE);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(JdbcMapper.mapPlace(rs));
            }
            return Optional.empty();
        }
    }

    /**
     * Passe une place de LIBRE à OCCUPEE si elle est encore libre (verrouillage optimiste).
     *
     * @return {@code true} si la mise à jour a réussi
     */
    public boolean occuper(Connection connection, int idPlace) throws SQLException {
        return updateStatutIfCurrent(connection, idPlace, StatutPlace.LIBRE, StatutPlace.OCCUPEE);
    }

    /**
     * Passe une place de OCCUPEE à LIBRE si elle est encore occupée (verrouillage optimiste).
     *
     * @return {@code true} si la mise à jour a réussi
     */
    public boolean liberer(Connection connection, int idPlace) throws SQLException {
        return updateStatutIfCurrent(connection, idPlace, StatutPlace.OCCUPEE, StatutPlace.LIBRE);
    }

    /**
     * Met à jour le statut d'une place uniquement si le statut actuel correspond à {@code expected}.
     */
    public boolean updateStatutIfCurrent(Connection connection, int idPlace,
                                         StatutPlace expected, StatutPlace newStatut) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_STATUT_IF_CURRENT)) {
            ps.setString(1, newStatut.name());
            ps.setInt(2, idPlace);
            ps.setString(3, expected.name());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Compte les places pour un statut donné (tableau de bord, §4.2 CDC).
     */
    public int countByStatut(StatutPlace statut) {
        return query(conn -> countByStatut(conn, statut));
    }

    public int countByStatut(Connection connection, StatutPlace statut) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(COUNT_BY_STATUT)) {
            ps.setString(1, statut.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("total");
            }
        }
    }
}
