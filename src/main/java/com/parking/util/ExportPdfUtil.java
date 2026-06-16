package com.parking.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.parking.model.Ticket;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExportPdfUtil {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ExportPdfUtil() {
    }

    public static void exportTickets(Path destination, List<Ticket> tickets) throws IOException {

        PdfWriter writer = new PdfWriter(destination.toString());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Titre
        Paragraph title = new Paragraph("Rapport des Tickets de Parking")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);

        document.add(title);

        // Date génération
        document.add(
                new Paragraph("Généré le : " +
                        LocalDateTime.now().format(DATE_FORMAT))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.RIGHT)
        );

        document.add(new Paragraph("\n"));

        // Tableau
        float[] columnWidths = {2, 1.5f, 2, 2.5f, 2.5f, 1.5f, 1.5f};

        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        // En-têtes
        addHeaderCell(table, "Ticket");
        addHeaderCell(table, "Place");
        addHeaderCell(table, "Immatriculation");
        addHeaderCell(table, "Entrée");
        addHeaderCell(table, "Sortie");
        addHeaderCell(table, "Montant");
        addHeaderCell(table, "Statut");

        // Données
        for (Ticket ticket : tickets) {

            table.addCell(createCell(ticket.getNumeroTicket()));
            table.addCell(createCell(String.valueOf(ticket.getIdPlace())));
            table.addCell(createCell(ticket.getImmatriculation()));
            table.addCell(createCell(formatDate(ticket.getDateEntree())));
            table.addCell(createCell(formatDate(ticket.getDateSortie())));
            table.addCell(createCell(formatMontant(ticket.getMontant())));

            String statut = ticket.getStatut() != null
                    ? ticket.getStatut().name()
                    : "";

            table.addCell(createCell(statut));
        }

        document.add(table);

        document.add(new Paragraph("\n"));

        // Résumé
        document.add(
                new Paragraph("Nombre total de tickets : " + tickets.size())
                        .setFontSize(12)
        );

        document.close();
    }

    private static void addHeaderCell(Table table, String text) {
        table.addHeaderCell(
                new Cell()
                        .add(new Paragraph(text))
                        .setBackgroundColor(ColorConstants.DARK_GRAY)
                        .setFontColor(ColorConstants.WHITE)
        );
    }

    private static Cell createCell(String value) {
        return new Cell()
                .add(new Paragraph(value != null ? value : ""))
                .setPadding(5);
    }

    private static String formatDate(LocalDateTime date) {
        return date != null
                ? date.format(DATE_FORMAT)
                : "";
    }

    private static String formatMontant(BigDecimal montant) {
        return montant != null
                ? montant.toPlainString() + " £"
                : "";
    }
}