package com.parking;

import com.parking.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests de base pour la Phase 1 — configuration applicative.
 */
class AppConfigTest {

    @Test
    void getInstance_chargeLaConfiguration() {
        AppConfig config = AppConfig.getInstance();
        assertNotNull(config);
        assertEquals("parking_moto", config.getDbName());
        assertEquals("parking_user", config.getDbUser());
        assertEquals("TKT", config.getTicketPrefixe());
    }
}
