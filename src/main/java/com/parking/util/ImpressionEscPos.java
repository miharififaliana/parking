package com.parking.util;

import com.parking.config.AppConfig;
import com.parking.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/**
 * Impression thermique ESC/POS (§4.3 CDC — désactivable via config.properties).
 * Compatible ports série Windows ({@code COM1}, {@code \\.\COM3}, etc.).
 */
public final class ImpressionEscPos {

    private static final Logger logger = LoggerFactory.getLogger(ImpressionEscPos.class);
    private static final Charset CP437 = Charset.forName("IBM437");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final AppConfig config;

    public ImpressionEscPos() {
        this(AppConfig.getInstance());
    }

    ImpressionEscPos(AppConfig config) {
        this.config = config;
    }

    public boolean imprimerTicket(Ticket ticket, int numeroPlace) {
        if (!config.isImpressionActive()) {
            logger.debug("Impression désactivée — ticket {} affiché à l'écran uniquement", ticket.getNumeroTicket());
            return false;
        }

        String port = config.getImpressionPort();
        if (port == null || port.isBlank()) {
            logger.warn("impression.port non configuré — impression ignorée");
            return false;
        }

        try {
            byte[] payload = buildEscPosPayload(ticket, numeroPlace);
            Path printerPath = resolveWindowsPort(port.trim());
            try (OutputStream out = Files.newOutputStream(printerPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                out.write(payload);
                out.flush();
            }
            logger.info("Ticket {} envoyé vers {}", ticket.getNumeroTicket(), printerPath);
            return true;
        } catch (IOException e) {
            logger.error("Échec impression ticket {} : {}", ticket.getNumeroTicket(), e.getMessage());
            return false;
        }
    }

    static Path resolveWindowsPort(String port) {
        if (port.startsWith("\\\\.\\")) {
            return Paths.get(port);
        }
        if (port.matches("(?i)COM\\d+")) {
            return Paths.get("\\\\.\\" + port.toUpperCase());
        }
        return Paths.get(port);
    }

    byte[] buildEscPosPayload(Ticket ticket, int numeroPlace) {
        int largeur = Math.max(16, config.getImpressionLargeur());
        StringBuilder sb = new StringBuilder();
        sb.append((char) 0x1B).append('@');
        sb.append(center(config.getParkingNom(), largeur)).append("\r\n");
        if (!config.getParkingAdresse().isBlank()) {
            sb.append(center(config.getParkingAdresse(), largeur)).append("\r\n");
        }
        sb.append(repeat('-', largeur)).append("\r\n");
        sb.append("Ticket N : ").append(ticket.getNumeroTicket()).append("\r\n");
        sb.append("Place     : ").append(numeroPlace).append("\r\n");
        sb.append("Immat.    : ").append(ticket.getImmatriculation()).append("\r\n");
        sb.append("Entree    : ").append(ticket.getDateEntree().format(DATE)).append("\r\n");
        sb.append("Heure     : ").append(ticket.getDateEntree().format(HEURE)).append("\r\n");
        sb.append(repeat('-', largeur)).append("\r\n");
        sb.append(center("[QR: " + ticket.getNumeroTicket() + "]", largeur)).append("\r\n");
        sb.append("\r\n\r\n");
        sb.append((char) 0x1D).append('V').append((char) 0x00);

        return sb.toString().getBytes(CP437);
    }

    private static String center(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private static String repeat(char c, int count) {
        return String.valueOf(c).repeat(count);
    }
}
