package com.parking.util;

import com.parking.model.Ticket;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export CSV des tickets (§4.6 CDC — séparateur point-virgule, CRLF Windows).
 */
public final class ExportCsvUtil {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LINE_SEP = "\r\n";

    private ExportCsvUtil() {
    }

    public static void exportTickets(Path destination, List<Ticket> tickets) throws IOException {
        try (Writer writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write("numero_ticket;id_place;immatriculation;date_entree;date_sortie;montant;statut");
            writer.write(LINE_SEP);
            for (Ticket ticket : tickets) {
                writer.write(escape(ticket.getNumeroTicket()));
                writer.write(';');
                writer.write(String.valueOf(ticket.getIdPlace()));
                writer.write(';');
                writer.write(escape(ticket.getImmatriculation()));
                writer.write(';');
                writer.write(format(ticket.getDateEntree()));
                writer.write(';');
                writer.write(format(ticket.getDateSortie()));
                writer.write(';');
                writer.write(formatMontant(ticket.getMontant()));
                writer.write(';');
                writer.write(escape(ticket.getStatut() != null ? ticket.getStatut().name() : ""));
                writer.write(LINE_SEP);
            }
        }
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO) : "";
    }

    private static String formatMontant(BigDecimal montant) {
        return montant != null ? montant.toPlainString() : "";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
