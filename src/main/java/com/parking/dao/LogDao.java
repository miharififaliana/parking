package com.parking.dao;

import com.parking.config.DatabaseConfig;
import com.parking.model.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC à la table {@code logs} — journal d'audit immuable (§5.4 CDC).
 */
public class LogDao extends BaseDao {

    private static final String INSERT = """
            INSERT INTO logs (id_user, action, detail, ip_address)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_DERNIERS = """
            SELECT id_log, id_user, action, detail, ip_address, created_at
            FROM logs
            ORDER BY created_at DESC
            LIMIT ?
            """;

    public LogDao() {
        super();
    }

    LogDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    /**
     * Enregistre une action dans le journal d'audit.
     *
     * @return l'identifiant généré ({@code id_log})
     */
    public int insert(Log log) {
        return executeInTransaction(conn -> insert(conn, log));
    }

    public int insert(Connection connection, Log log) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getIdUser());
            ps.setString(2, log.getAction());
            ps.setString(3, log.getDetail());
            ps.setString(4, log.getIpAddress());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Insertion log échouée — lignes affectées : " + rows);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    log.setIdLog(id);
                    return id;
                }
                throw new SQLException("Aucune clé générée après insertion du log");
            }
        }
    }

    /**
     * Retourne les N dernières entrées du journal d'audit.
     */
    public List<Log> findDerniers(int limit) {
        return query(conn -> findDerniers(conn, limit));
    }

    public List<Log> findDerniers(Connection connection, int limit) throws SQLException {
        List<Log> logs = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_DERNIERS)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(JdbcMapper.mapLog(rs));
                }
            }
        }
        return logs;
    }
}
