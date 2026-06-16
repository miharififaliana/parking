package com.parking.controller;

import com.parking.config.AppConfig;
import com.parking.model.Ticket;
import com.parking.util.ImpressionEscPos;
import com.parking.util.QrCodeGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

/**
 * Aperçu du ticket avant/après impression (§7 CDC).
 */
public class TicketPreviewController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label parkingNomLabel;
    @FXML private Label ticketLabel;
    @FXML private Label placeLabel;
    @FXML private Label immatLabel;
    @FXML private Label dateLabel;
    @FXML private Label heureLabel;
    @FXML private Label adresseLabel;
    @FXML private ImageView qrImageView;

    private Ticket ticket;
    private int numeroPlace;

    @FXML
    private void initialize() {
        AppConfig config = AppConfig.getInstance();
        parkingNomLabel.setText(config.getParkingNom());
        adresseLabel.setText(config.getParkingAdresse());
    }

    public void init(Ticket ticket, int numeroPlace) {
        this.ticket = ticket;
        this.numeroPlace = numeroPlace;

        ticketLabel.setText(ticket.getNumeroTicket());
        placeLabel.setText(String.valueOf(numeroPlace));
        immatLabel.setText(ticket.getImmatriculation());
        dateLabel.setText(ticket.getDateEntree().format(DATE));
        heureLabel.setText(ticket.getDateEntree().format(HEURE));
        qrImageView.setImage(QrCodeGenerator.generateFxImage(ticket.getNumeroTicket(), 160));
    }

    @FXML
    private void onImprimer() {
        if (ticket != null) {
            new ImpressionEscPos().imprimerTicket(ticket, numeroPlace);
        }
    }

    @FXML
    private void onFermer() {
        Stage stage = (Stage) ticketLabel.getScene().getWindow();
        stage.close();
    }
}
