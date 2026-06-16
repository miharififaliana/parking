package com.parking.controller;

import com.parking.dao.PlaceDao;
import com.parking.exception.DatabaseException;
import com.parking.exception.ParkingCompletException;
import com.parking.exception.ValidationException;
import com.parking.model.Place;
import com.parking.model.Ticket;
import com.parking.service.TicketService;
import com.parking.util.ImpressionEscPos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Enregistrement d'entrée véhicule (§3.3 / §4.3 CDC).
 */
public class EntreeController {

    private static final Logger logger = LoggerFactory.getLogger(EntreeController.class);

    @FXML private Label placeLabel;
    @FXML private Label statusLabel;
    @FXML private TextField immatField;
    @FXML private Button validerButton;
    @FXML private Button rechercherButton;

    private final TicketService ticketService = new TicketService();
    private final PlaceDao placeDao = new PlaceDao();
    private final ImpressionEscPos impression = new ImpressionEscPos();
    private Optional<Place> placeProposee = Optional.empty();

    @FXML
    private void initialize() {
        statusLabel.setText("");
        immatField.setOnAction(e -> onValider());
        rechercherPlace();
    }

    @FXML
    private void onRechercherPlace() {
        rechercherPlace();
    }

    @FXML
    private void onValider() {
        clearStatus();
        if (placeProposee.isEmpty()) {
            showError("Aucune place disponible — parking complet.");
            return;
        }

        validerButton.setDisable(true);
        try {
            Ticket ticket = ticketService.enregistrerEntree(immatField.getText());
            Place place = placeDao.findById(ticket.getIdPlace()).orElseThrow();
            showSuccess("Entrée enregistrée — ticket " + ticket.getNumeroTicket());
            immatField.clear();
            impression.imprimerTicket(ticket, place.getNumeroPlace());
            afficherApercuTicket(ticket, place.getNumeroPlace());
            rechercherPlace();
        } catch (ParkingCompletException e) {
            showError(e.getMessage());
            rechercherPlace();
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (DatabaseException e) {
            logger.error("Erreur entrée : {}", e.getMessage());
            SceneNavigator.showDatabaseError(e);
            showError("Erreur base de données.");
        } catch (RuntimeException e) {
            logger.error("Erreur entrée : {}", e.getMessage(), e);
            showError("Erreur : " + e.getMessage());
        } finally {
            validerButton.setDisable(false);
        }
    }

    private void rechercherPlace() {
        clearStatus();
        try {
            placeProposee = ticketService.proposerPlaceLibre();
            if (placeProposee.isPresent()) {
                placeLabel.setText("Place n°" + placeProposee.get().getNumeroPlace());
                placeLabel.getStyleClass().removeAll("place-libre", "place-complet");
                placeLabel.getStyleClass().add("place-libre");
                validerButton.setDisable(false);
            } else {
                placeLabel.setText("Parking complet");
                placeLabel.getStyleClass().removeAll("place-libre", "place-complet");
                placeLabel.getStyleClass().add("place-complet");
                validerButton.setDisable(true);
            }
        } catch (DatabaseException e) {
            SceneNavigator.showDatabaseError(e);
            showError("Impossible de rechercher une place.");
        }
    }

    private void afficherApercuTicket(Ticket ticket, int numeroPlace) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TicketPreview.fxml"));
            Parent root = loader.load();
            TicketPreviewController controller = loader.getController();
            controller.init(ticket, numeroPlace);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(SceneNavigator.getPrimaryStage());
            stage.setTitle("Ticket — " + ticket.getNumeroTicket());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            logger.warn("Aperçu ticket indisponible : {}", e.getMessage());
        }
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
