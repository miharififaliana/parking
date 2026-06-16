package com.parking.dao;

import com.parking.config.AppConfig;
import com.parking.config.DatabaseConfig;
import com.parking.model.SequenceTicket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.Optional;

/**
 * Accès JDBC à la table {@code sequence_tickets}.
 *
 * <p>Gère la numérotation transactionnelle des tickets au format TKT-AAAA-NNNN (§4.3 CDC)
 * via {@code SELECT ... FOR UPDATE}.</p>
 */
public class SequenceTicketDao extends BaseDao {

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO sequence_tickets (annee, derniere_sequence)
            VALUES (?, 0)
            ON DUPLICATE KEY UPDATE annee = annee
            """;

    private static final String SELECT_FOR_UPDATE = """
            SELECT annee, derniere_sequence, updated_at
            FROM sequence_tickets
            WHERE annee = ?
            FOR UPDATE
            """;

    private static final String UPDATE_SEQUENCE = """
            UPDATE sequence_tickets
            SET derniere_sequence = ?
            WHERE annee = ?
            """;

    public SequenceTicketDao() {
        super();
    }

    SequenceTicketDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    /**
     * Génère le prochain numéro de ticket complet (ex. TKT-2026-0001).
     * Doit être appelé dans une transaction existante.
     */
    public String nextNumeroTicket(Connection connection) throws SQLException {
        int annee = Year.now().getValue();
        int sequence = nextSequence(connection, annee);
        return formatNumeroTicket(annee, sequence);
    }

    /**
     * Génère le prochain numéro de ticket dans une transaction autonome.
     */
    public String nextNumeroTicket() {
        return executeInTransaction(this::nextNumeroTicket);
    }

    /**
     * Incrémente et retourne la prochaine séquence pour une année donnée.
     * Utilise {@code SELECT FOR UPDATE} pour garantir l'unicité en concurrence.
     */
    public int nextSequence(Connection connection, int annee) throws SQLException {
        ensureAnneeExists(connection, annee);

        SequenceTicket current = lockSequence(connection, annee)
                .orElseThrow(() -> new SQLException("Séquence introuvable pour l'année " + annee));

        int next = current.getDerniereSequence() + 1;

        try (PreparedStatement ps = connection.prepareStatement(UPDATE_SEQUENCE)) {
            ps.setInt(1, next);
            ps.setInt(2, annee);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Mise à jour de la séquence échouée pour l'année " + annee);
            }
        }

        return next;
    }

    /**
     * Formate un numéro de ticket selon la configuration (préfixe + longueur de séquence).
     */
    public String formatNumeroTicket(int annee, int sequence) {
        AppConfig config = AppConfig.getInstance();
        String prefix = config.getTicketPrefixe();
        int length = config.getTicketSequenceLength();
        return String.format("%s-%d-%0" + length + "d", prefix, annee, sequence);
    }

    /**
     * Lit la séquence courante pour une année (sans incrément).
     */
    public Optional<SequenceTicket> findByAnnee(int annee) {
        return query(conn -> findByAnnee(conn, annee));
    }

    public Optional<SequenceTicket> findByAnnee(Connection connection, int annee) throws SQLException {
        ensureAnneeExists(connection, annee);
        return lockSequence(connection, annee);
    }

    private void ensureAnneeExists(Connection connection, int annee) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_IF_ABSENT)) {
            ps.setInt(1, annee);
            ps.executeUpdate();
        }
    }

    private Optional<SequenceTicket> lockSequence(Connection connection, int annee) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_FOR_UPDATE)) {
            ps.setInt(1, annee);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(JdbcMapper.mapSequenceTicket(rs));
                }
                return Optional.empty();
            }
        }
    }
}
