package com.parking.controller;

import com.parking.exception.DatabaseException;
import com.parking.model.Place;
import com.parking.model.StatutPlace;
import com.parking.service.PlaceService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Grille visuelle des places (§4.2 CDC).
 */
public class PlacesController {

    private static final Logger logger = LoggerFactory.getLogger(PlacesController.class);

    @FXML private Label totalLabel;
    @FXML private Label libresLabel;
    @FXML private Label occupeesLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane placesGrid;

    private final PlaceService placeService = new PlaceService();

    @FXML
    private void initialize() {
        statusLabel.setText("");
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        clearStatus();
        try {
            var places = placeService.getAllPlaces();
            totalLabel.setText(String.valueOf(placeService.countTotal()));
            libresLabel.setText(String.valueOf(placeService.countLibres()));
            occupeesLabel.setText(String.valueOf(placeService.countOccupees()));

            placesGrid.getChildren().clear();
            for (Place place : places) {
                placesGrid.getChildren().add(createPlaceCell(place));
            }
        } catch (DatabaseException e) {
            logger.warn("Erreur grille places : {}", e.getMessage());
            SceneNavigator.showDatabaseError(e);
            showError("Impossible de charger les places.");
        }
    }

    private VBox createPlaceCell(Place place) {
        boolean libre = place.getStatut() == StatutPlace.LIBRE;
        VBox cell = new VBox(4);
        cell.getStyleClass().addAll("place-cell", libre ? "place-cell-libre" : "place-cell-occupee");
        cell.setPrefSize(72, 56);

        Label numero = new Label(String.valueOf(place.getNumeroPlace()));
        numero.getStyleClass().add("place-cell-numero");

        Label statut = new Label(libre ? "LIBRE" : "OCC.");
        statut.getStyleClass().add("place-cell-statut");

        cell.getChildren().addAll(numero, statut);
        return cell;
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().add("status-error");
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.getStyleClass().remove("status-error");
    }
}
