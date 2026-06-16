package com.parking.dao;

import com.parking.config.DatabaseConfig;
import com.parking.model.StatutTicket;
import com.parking.model.Ticket;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Accès JDBC à la table {@code tickets} (§3.3 / §3.4 / §4.3 / §4.4 CDC).
 */
public class TicketDao extends BaseDao {

    private static final String INSERT = """
            INSERT INTO tickets (numero_ticket, id_place, immatriculation, date_entree, statut)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SORTIE = """
            UPDATE tickets
            SET date_sortie = ?, montant = ?, statut = 'PAYE'
            WHERE id_ticket = ?
              AND statut = 'ACTIF'
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_ticket, numero_ticket, id_place, immatriculation,
                   date_entree, date_sortie, montant, statut, created_at, updated_at
            FROM tickets
            WHERE id_ticket = ?
            """;

    private static final String SELECT_BY_NUMERO = """
            SELECT id_ticket, numero_ticket, id_place, immatriculation,
                   date_entree, date_sortie, montant, statut, created_at, updated_at
            FROM tickets
            WHERE numero_ticket = ?
            """;

    private static final String SELECT_ACTIF_BY_PLACE = """
            SELECT id_ticket, numero_ticket, id_place, immatriculation,
                   date_entree, date_sortie, montant, statut, created_at, updated_at
            FROM tickets
            WHERE id_place = ?
              AND statut = 'ACTIF'
            LIMIT 1
            """;

    private static final String SELECT_DERNIERS = """
            SELECT id_ticket, numero_ticket, id_place, immatriculation,
                   date_entree, date_sortie, montant, statut, created_at, updated_at
            FROM tickets
            ORDER BY COALESCE(date_sortie, date_entree) DESC
            LIMIT ?
            """;

    private static final String SUM_MONTANT_PAYE = """
            SELECT COALESCE(SUM(montant), 0) AS total
            FROM tickets
            WHERE statut = 'PAYE'
              AND date_sortie >= ?
              AND date_sortie < ?
            """;

    private static final String SELECT_ALL = """
            SELECT id_ticket, numero_ticket, id_place, immatriculation,
                   date_entree, date_sortie, montant, statut, created_at, updated_at
            FROM tickets
            ORDER BY date_entree DESC
            """;

    private static final String COUNT_ENTREES = """
            SELECT COUNT(*) AS total
            FROM tickets
            WHERE date_entree >= ?
              AND date_entree < ?
            """;

    private static final String AVG_DUREE_PAYES = """
            SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, date_entree, date_sortie)), 0) AS moyenne
            FROM tickets
            WHERE statut = 'PAYE'
              AND date_sortie IS NOT NULL
              AND date_sortie >= ?
              AND date_sortie < ?
            """;

    private static final String SUM_MONTANT_PAR_JOUR = """
            SELECT DATE(date_sortie) AS jour, COALESCE(SUM(montant), 0) AS total
            FROM tickets
            WHERE statut = 'PAYE'
              AND date_sortie >= ?
              AND date_sortie < ?
            GROUP BY DATE(date_sortie)
            ORDER BY jour ASC
            """;

    private static final String COUNT_ENTREES_PAR_HEURE = """
            SELECT HOUR(date_entree) AS heure, COUNT(*) AS total
            FROM tickets
            WHERE date_entree >= ?
              AND date_entree < ?
            GROUP BY HOUR(date_entree)
            ORDER BY heure ASC
            """;

    public TicketDao() {
        super();
    }

    TicketDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    /**
     * Insère un ticket d'entrée (statut ACTIF).
     *
     * @return l'identifiant généré ({@code id_ticket})
     */
    public int insert(Ticket ticket) {
        return executeInTransaction(conn -> insert(conn, ticket));
    }

    public int insert(Connection connection, Ticket ticket) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ticket.getNumeroTicket());
            ps.setInt(2, ticket.getIdPlace());
            ps.setString(3, ticket.getImmatriculation());
            ps.setTimestamp(4, JdbcMapper.toTimestamp(ticket.getDateEntree()));
            ps.setString(5, ticket.getStatut() != null ? ticket.getStatut().name() : StatutTicket.ACTIF.name());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Insertion ticket échouée — lignes affectées : " + rows);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    ticket.setIdTicket(id);
                    return id;
                }
                throw new SQLException("Aucune clé générée après insertion du ticket");
            }
        }
    }

    /**
     * Archive un ticket à la sortie : date de sortie, montant et statut PAYE (§3.4 CDC).
     *
     * @return {@code true} si le ticket était ACTIF et a été mis à jour
     */
    public boolean updateForSortie(int idTicket, LocalDateTime dateSortie, BigDecimal montant) {
        return executeInTransaction(conn -> updateForSortie(conn, idTicket, dateSortie, montant));
    }

    public boolean updateForSortie(Connection connection, int idTicket,
                                   LocalDateTime dateSortie, BigDecimal montant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_SORTIE)) {
            ps.setTimestamp(1, JdbcMapper.toTimestamp(dateSortie));
            ps.setBigDecimal(2, montant);
            ps.setInt(3, idTicket);
            return ps.executeUpdate() == 1;
        }
    }

    public Optional<Ticket> findById(int idTicket) {
        return query(conn -> findById(conn, idTicket));
    }

    public Optional<Ticket> findById(Connection connection, int idTicket) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, idTicket);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapTicket(rs));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<Ticket> findByNumeroTicket(String numeroTicket) {
        return query(conn -> findByNumeroTicket(conn, numeroTicket));
    }

    public Optional<Ticket> findByNumeroTicket(Connection connection, String numeroTicket) throws SQLException {
        if (numeroTicket == null || numeroTicket.isBlank()) {
            return Optional.empty();
        }

        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_NUMERO)) {
            ps.setString(1, numeroTicket.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapTicket(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Recherche le ticket actif associé à une place (§4.4 CDC — recherche par numéro de place).
     */
    public Optional<Ticket> findActifByIdPlace(int idPlace) {
        return query(conn -> findActifByIdPlace(conn, idPlace));
    }

    public Optional<Ticket> findActifByIdPlace(Connection connection, int idPlace) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ACTIF_BY_PLACE)) {
            ps.setInt(1, idPlace);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapTicket(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Retourne les N derniers mouvements (entrées ou sorties) pour le tableau de bord (§4.5 CDC).
     */
    public List<Ticket> findDerniersMouvements(int limit) {
        return query(conn -> findDerniersMouvements(conn, limit));
    }

    public List<Ticket> findDerniersMouvements(Connection connection, int limit) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_DERNIERS)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(JdbcMapper.mapTicket(rs));
                }
            }
        }
        return tickets;
    }

    /**
     * Somme des montants encaissés (tickets PAYE) sur une période semi-ouverte [{@code debut}, {@code fin}).
     */
    public BigDecimal sumMontantPayeBetween(LocalDateTime debut, LocalDateTime fin) {
        return query(conn -> sumMontantPayeBetween(conn, debut, fin));
    }

    public BigDecimal sumMontantPayeBetween(Connection connection,
                                            LocalDateTime debut, LocalDateTime fin) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SUM_MONTANT_PAYE)) {
            ps.setTimestamp(1, JdbcMapper.toTimestamp(debut));
            ps.setTimestamp(2, JdbcMapper.toTimestamp(fin));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        }
    }

    public List<Ticket> findAll() {
        return query(this::findAll);
    }

    public List<Ticket> findAll(Connection connection) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tickets.add(JdbcMapper.mapTicket(rs));
            }
        }
        return tickets;
    }

    public int countEntreesBetween(LocalDateTime debut, LocalDateTime fin) {
        return query(conn -> countEntreesBetween(conn, debut, fin));
    }

    public int countEntreesBetween(Connection connection, LocalDateTime debut, LocalDateTime fin)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(COUNT_ENTREES)) {
            ps.setTimestamp(1, JdbcMapper.toTimestamp(debut));
            ps.setTimestamp(2, JdbcMapper.toTimestamp(fin));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("total");
            }
        }
    }

    public double avgDureeMinutesPayesBetween(LocalDateTime debut, LocalDateTime fin) {
        return query(conn -> avgDureeMinutesPayesBetween(conn, debut, fin));
    }

    public double avgDureeMinutesPayesBetween(Connection connection, LocalDateTime debut, LocalDateTime fin)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(AVG_DUREE_PAYES)) {
            ps.setTimestamp(1, JdbcMapper.toTimestamp(debut));
            ps.setTimestamp(2, JdbcMapper.toTimestamp(fin));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("moyenne");
            }
        }
    }

    public List<com.parking.model.StatsSnapshot.RecetteJour> sumMontantPayeGroupByDay(
            LocalDateTime debut, LocalDateTime fin) {
        return query(conn -> sumMontantPayeGroupByDay(conn, debut, fin));
    }

    public List<com.parking.model.StatsSnapshot.RecetteJour> sumMontantPayeGroupByDay(
            Connection connection, LocalDateTime debut, LocalDateTime fin) throws SQLException {
        List<com.parking.model.StatsSnapshot.RecetteJour> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SUM_MONTANT_PAR_JOUR)) {
            ps.setTimestamp(1, JdbcMapper.toTimestamp(debut));
            ps.setTimestamp(2, JdbcMapper.toTimestamp(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String jour = rs.getDate("jour").toLocalDate().toString();
                    result.add(new com.parking.model.StatsSnapshot.RecetteJour(
                            jour, rs.getBigDecimal("total")));
                }
            }
        }
        return result;
    }

    public Map<Integer, Integer> countEntreesGroupByHour(LocalDateTime debut, LocalDateTime fin) {
        return query(conn -> countEntreesGroupByHour(conn, debut, fin));
    }

    public Map<Integer, Integer> countEntreesGroupByHour(Connection connection,
                                                         LocalDateTime debut, LocalDateTime fin)
            throws SQLException {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            result.put(h, 0);
        }
        try (PreparedStatement ps = connection.prepareStatement(COUNT_ENTREES_PAR_HEURE)) {
            ps.setTimestamp(1, JdbcMapper.toTimestamp(debut));
            ps.setTimestamp(2, JdbcMapper.toTimestamp(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("heure"), rs.getInt("total"));
                }
            }
        }
        return result;
    }
}
