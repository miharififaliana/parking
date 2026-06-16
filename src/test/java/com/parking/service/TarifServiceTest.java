package com.parking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires du calcul tarifaire (§3.2 CDC — sans base de données).
 */
class TarifServiceTest {

    private TarifService tarifService;

    @BeforeEach
    void setUp() {
        tarifService = new TarifService();
    }

    @ParameterizedTest(name = "{0} min → {1} h facturée(s) → {2} €")
    @CsvSource({
            "30, 1, 1.00",
            "65, 2, 2.00",
            "180, 3, 3.00",
            "0, 1, 1.00",
            "60, 1, 1.00",
            "61, 2, 2.00"
    })
    void calculerMontant_respecteLaRegleCeilParHeure(long minutes, long heuresAttendues, String montantAttendu) {
        LocalDateTime entree = LocalDateTime.of(2026, 6, 13, 8, 0);
        LocalDateTime sortie = entree.plusMinutes(minutes);

        assertEquals(heuresAttendues, tarifService.calculerHeuresFacturees(minutes));
        assertEquals(new BigDecimal(montantAttendu), tarifService.calculerMontant(entree, sortie));
    }

    @org.junit.jupiter.api.Test
    void calculerDureeMinutes_retourneLaDifferenceExacte() {
        LocalDateTime entree = LocalDateTime.of(2026, 6, 13, 10, 15);
        LocalDateTime sortie = LocalDateTime.of(2026, 6, 13, 11, 20);

        assertEquals(65, tarifService.calculerDureeMinutes(entree, sortie));
    }
}
