package com.parking.controller;

import com.parking.exception.DatabaseException;
import com.parking.exception.TicketIntrouvableException;
import com.parking.exception.ValidationException;
import com.parking.model.Ticket;
import com.parking.service.TicketService;
import com.parking.util.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Enregistrement de sortie et encaissement (§3.4 / §4.4 CDC).
 */
public class SortieController {

    private static final Logger logger = LoggerFactory.getLogger(SortieController.class);

    @FXML private TextField ticketField;
    @FXML private TextField placeField;
    @FXML private Label immatLabel;
    @FXML private Label entreeLabel;
    @FXML private Label dureeLabel;
    @FXML private Label montantLabel;
    @FXML private Label statusLabel;
    @FXML private Button validerButton;

    private final TicketService ticketService = new TicketService();
    private String ticketActif;

    @FXML
    private void initialize() {
        statusLabel.setText("");
        resetDetails();
        ticketField.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) {
                placeField.clear();
            }
        });
        placeField.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) {
                ticketField.clear();
            }
        });
    }

    @FXML
    private void onRechercher() {
        clearStatus();
        resetDetails();
        ticketActif = null;

        String numeroTicket = ticketField.getText().trim();
        String numeroPlace = placeField.getText().trim();

        if (numeroTicket.isEmpty() && numeroPlace.isEmpty()) {
            showError("Saisissez un numéro de ticket ou de place.");
            return;
        }

        try {
            if (!numeroTicket.isEmpty()) {
                chargerApercu(numeroTicket);
            } else {
                int place = Integer.parseInt(numeroPlace);
                var ticketOpt = new com.parking.dao.TicketDao().findActifByIdPlace(
                        new com.parking.dao.PlaceDao().findByNumeroPlace(place)
                                .orElseThrow(() -> new TicketIntrouvableException("Place introuvable : " + place))
                                .getIdPlace());
                if (ticketOpt.isEmpty()) {
                    throw new TicketIntrouvableException("Aucun ticket actif sur la place " + place);
                }
                ticketActif = ticketOpt.get().getNumeroTicket();
                ticketField.setText(ticketActif);
                chargerApercu(ticketActif);
            }
        } catch (NumberFormatException e) {
            showError("Numéro de place invalide.");
        } catch (TicketIntrouvableException e) {
            showError(e.getMessage());
        } catch (DatabaseException e) {
            SceneNavigator.showDatabaseError(e);
            showError("Erreur base de données.");
        }
    }

    @FXML
    private void onValiderPaiement() {
        clearStatus();
        if (ticketActif == null || ticketActif.isBlank()) {
            showError("Recherchez d'abord un ticket actif.");
            return;
        }

        validerButton.setDisable(true);
        try {
            Ticket ticket = ticketService.enregistrerSortie(ticketActif);
            showSuccess(String.format("Paiement encaissé — %.2f € — place libérée.",
                    ticket.getMontant()));
            ticketField.clear();
            placeField.clear();
            resetDetails();
            ticketActif = null;
        } catch (TicketIntrouvableException | ValidationException e) {
            showError(e.getMessage());
        } catch (DatabaseException e) {
            SceneNavigator.showDatabaseError(e);
            showError("Erreur base de données.");
        } catch (RuntimeException e) {
            logger.error("Erreur sortie : {}", e.getMessage(), e);
            showError("Erreur : " + e.getMessage());
        } finally {
            validerButton.setDisable(false);
        }
    }

    private void chargerApercu(String numeroTicket) {
        var ticket = new com.parking.dao.TicketDao().findByNumeroTicket(numeroTicket)
                .filter(Ticket::isActif)
                .orElseThrow(() -> new TicketIntrouvableException(
                        "Aucun ticket actif pour : " + numeroTicket));

        ticketActif = numeroTicket;
        long minutes = ticketService.calculerDureeMinutes(numeroTicket);
        BigDecimal montant = ticketService.calculerMontantSortie(numeroTicket);

        immatLabel.setText(ticket.getImmatriculation());
        entreeLabel.setText(ticket.getDateEntree().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dureeLabel.setText(DateUtils.formatDuree(minutes));
        montantLabel.setText(String.format("%.2f €", montant));
        validerButton.setDisable(false);
    }

    private void resetDetails() {
        immatLabel.setText("—");
        entreeLabel.setText("—");
        dureeLabel.setText("—");
        montantLabel.setText("—");
        validerButton.setDisable(true);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("status-success");
        statusLabel.getStyleClass().add("status-error");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("status-error");
        statusLabel.getStyleClass().add("status-success");
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
    }
}
