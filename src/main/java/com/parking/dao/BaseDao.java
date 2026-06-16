package com.parking.dao;

import com.parking.config.DatabaseConfig;
import com.parking.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Classe de base des DAO JDBC.
 *
 * <p>Centralise le cycle de vie des connexions, le commit et le rollback
 * conformément au §9 CDC (transactions explicites, protection anti-coupure).</p>
 */
public abstract class BaseDao {

    protected final DatabaseConfig databaseConfig;

    protected BaseDao() {
        this(DatabaseConfig.getInstance());
    }

    protected BaseDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    /**
     * Emprunte une connexion du pool (auto-commit désactivé).
     */
    protected Connection borrowConnection() {
        return databaseConfig.getConnection();
    }

    /**
     * Rend une connexion au pool (rollback automatique si non commitée).
     */
    protected void releaseConnection(Connection connection) {
        databaseConfig.releaseConnection(connection);
    }

    /**
     * Valide la transaction en cours.
     */
    protected void commit(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors du commit de la transaction JDBC", e);
        }
    }

    /**
     * Annule la transaction en cours.
     */
    protected void rollback(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors du rollback de la transaction JDBC", e);
        }
    }

    /**
     * Exécute une opération en lecture seule.
     */
    protected <T> T query(SqlFunction<T> action) {
        Connection connection = borrowConnection();
        try {
            return action.apply(connection);
        } catch (SQLException e) {
            throw toDatabaseException(e);
        } finally {
            releaseConnection(connection);
        }
    }

    /**
     * Exécute une opération d'écriture avec commit automatique.
     */
    protected void executeUpdate(SqlConsumer action) {
        executeInTransaction(conn -> {
            action.accept(conn);
            return null;
        });
    }

    /**
     * Exécute une opération dans une transaction isolée avec commit automatique.
     */
    protected <T> T executeInTransaction(SqlFunction<T> action) {
        Connection connection = borrowConnection();
        try {
            T result = action.apply(connection);
            commit(connection);
            return result;
        } catch (SQLException e) {
            rollback(connection);
            throw toDatabaseException(e);
        } finally {
            releaseConnection(connection);
        }
    }

    protected DatabaseException toDatabaseException(SQLException exception) {
        return new DatabaseException("Erreur SQL : " + exception.getMessage(), exception);
    }

    @FunctionalInterface
    protected interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    protected interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
