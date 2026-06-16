package com.parking.controller;

import com.parking.exception.DatabaseException;
import com.parking.model.DashboardSnapshot;
import com.parking.service.DashboardService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Contrôleur du tableau de bord avec rafraîchissement périodique (§4.5 CDC — polling 5000 ms).
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HEURE_REFRESH = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label totalPlacesLabel;
    @FXML private Label libresLabel;
    @FXML private Label occupeesLabel;
    @FXML private Label tauxLabel;
    @FXML private ProgressBar occupationBar;
    @FXML private Label recetteJourLabel;
    @FXML private Label recetteSemaineLabel;
    @FXML private Label recetteMoisLabel;
    @FXML private Label lastRefreshLabel;
    @FXML private Label errorLabel;
    @FXML private TableView<DashboardSnapshot.MouvementResume> mouvementsTable;

    @FXML private TableColumn<DashboardSnapshot.MouvementResume, String> typeColumn;
    @FXML private TableColumn<DashboardSnapshot.MouvementResume, String> ticketColumn;
    @FXML private TableColumn<DashboardSnapshot.MouvementResume, String> placeColumn;
    @FXML private TableColumn<DashboardSnapshot.MouvementResume, String> immatColumn;
    @FXML private TableColumn<DashboardSnapshot.MouvementResume, String> dateColumn;
    @FXML private TableColumn<DashboardSnapshot.MouvementResume, String> montantColumn;

    private final DashboardService dashboardService = new DashboardService();
    private Timeline refreshTimeline;

    @FXML
    private void initialize() {
        configureMouvementsTable();
        errorLabel.setText("");
        refresh();
    }

    /**
     * Démarre le polling configuré ({@code ui.dashboard.refreshInterval}, défaut 5000 ms).
     */
    public void startRefreshing() {
        stopRefreshing();
        int intervalMs = SceneNavigator.getAppConfig().getDashboardRefreshMs();
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMs), e -> refresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        logger.debug("Polling tableau de bord démarré — intervalle {} ms", intervalMs);
    }

    /**
     * Arrête le polling (déconnexion ou changement d'écran).
     */
    public void stopRefreshing() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    @FXML
    private void onRefreshManual() {
        refresh();
    }

    private void refresh() {
        Platform.runLater(() -> {
            try {
                DashboardSnapshot snapshot = dashboardService.getSnapshot();
                applySnapshot(snapshot);
                errorLabel.setText("");
            } catch (DatabaseException e) {
                logger.warn("Erreur lors du rafraîchissement du tableau de bord : {}", e.getMessage());
                errorLabel.setText("Impossible de rafraîchir les données — vérifiez MySQL.");
            }
        });
    }

    private void applySnapshot(DashboardSnapshot snapshot) {
        totalPlacesLabel.setText(String.valueOf(snapshot.getTotalPlaces()));
        libresLabel.setText(String.valueOf(snapshot.getPlacesLibres()));
        occupeesLabel.setText(String.valueOf(snapshot.getPlacesOccupees()));
        tauxLabel.setText(String.format("%.0f %%", snapshot.getTauxOccupation()));

        occupationBar.setProgress(snapshot.getTauxOccupation() / 100.0);
        occupationBar.getStyleClass().removeAll("progress-ok", "progress-warning", "progress-full");
        if (snapshot.getTauxOccupation() >= 90) {
            occupationBar.getStyleClass().add("progress-full");
        } else if (snapshot.getTauxOccupation() >= 70) {
            occupationBar.getStyleClass().add("progress-warning");
        } else {
            occupationBar.getStyleClass().add("progress-ok");
        }

        recetteJourLabel.setText(formatEuros(snapshot.getRecetteJour()));
        recetteSemaineLabel.setText(formatEuros(snapshot.getRecetteSemaine()));
        recetteMoisLabel.setText(formatEuros(snapshot.getRecetteMois()));

        mouvementsTable.setItems(FXCollections.observableArrayList(snapshot.getDerniersMouvements()));
        lastRefreshLabel.setText("Dernière mise à jour : " + snapshot.getHorodatage().format(HEURE_REFRESH));
    }

    private void configureMouvementsTable() {
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        ticketColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumeroTicket()));
        placeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getNumeroPlace())));
        immatColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImmatriculation()));
        dateColumn.setCellValueFactory(data -> {
            var date = data.getValue().getDateHeure();
            return new SimpleStringProperty(date != null ? date.format(DATE_HEURE) : "—");
        });
        montantColumn.setCellValueFactory(data -> {
            BigDecimal montant = data.getValue().getMontant();
            return new SimpleStringProperty(montant != null ? formatEuros(montant) : "—");
        });
        mouvementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private static String formatEuros(BigDecimal montant) {
        if (montant == null) {
            return "0,00 €";
        }
        return String.format("%.2f €", montant);
    }
}
