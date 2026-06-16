package com.parking.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Formatage des durées pour l'interface (§4.4 CDC).
 */
public final class DateUtils {

    private DateUtils() {
    }

    public static String formatDuree(long minutes) {
        if (minutes < 0) {
            minutes = 0;
        }
        long heures = minutes / 60;
        long mins = minutes % 60;
        if (heures > 0) {
            return heures + " h " + mins + " min";
        }
        return mins + " min";
    }

    public static long minutesBetween(LocalDateTime debut, LocalDateTime fin) {
        return Duration.between(debut, fin).toMinutes();
    }
}
