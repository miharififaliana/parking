package com.parking.service;

import com.parking.config.DatabaseConfig;
import com.parking.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Classe de base des services métier orchestrant des transactions JDBC multi-DAO.
 */
public abstract class BaseService {

    protected final DatabaseConfig databaseConfig;

    protected BaseService() {
        this(DatabaseConfig.getInstance());
    }

    protected BaseService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    /**
     * Exécute une opération dans une transaction isolée avec commit automatique.
     */
    protected <T> T executeInTransaction(SqlFunction<T> action) {
        Connection connection = databaseConfig.getConnection();
        try {
            T result = action.apply(connection);
            connection.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new DatabaseException("Erreur SQL : " + e.getMessage(), e);
        } catch (RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            databaseConfig.releaseConnection(connection);
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // rollback best-effort
        }
    }

    @FunctionalInterface
    protected interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
