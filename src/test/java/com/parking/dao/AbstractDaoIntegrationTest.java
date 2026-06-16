package com.parking.dao;

import com.parking.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base des tests d'intégration JDBC nécessitant une instance MySQL opérationnelle.
 */
public abstract class AbstractDaoIntegrationTest {

    protected static DatabaseConfig databaseConfig;
    private static boolean poolInitialized;

    @BeforeAll
    static void initDatabasePool() {
        if (poolInitialized) {
            return;
        }
        databaseConfig = DatabaseConfig.getInstance();
        try {
            databaseConfig.testConnection();
            databaseConfig.initialize();
            poolInitialized = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (databaseConfig != null) {
                    databaseConfig.closePool();
                }
            }));
        } catch (RuntimeException e) {
            Assumptions.assumeTrue(false, "MySQL indisponible — tests DAO ignorés : " + e.getMessage());
        }
    }

    /**
     * Exécute une action JDBC puis annule la transaction pour ne pas polluer la base de test.
     */
    protected void executeThenRollback(SqlWork work) throws SQLException {
        Connection connection = databaseConfig.getConnection();
        try {
            work.run(connection);
        } finally {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // rollback best-effort avant remise au pool
            }
            databaseConfig.releaseConnection(connection);
        }
    }

    @FunctionalInterface
    protected interface SqlWork {
        void run(Connection connection) throws SQLException;
    }
}
