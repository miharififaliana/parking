package com.parking.controller;

import com.parking.exception.DatabaseException;
import com.parking.model.StatsSnapshot;
import com.parking.service.StatsService;
import com.parking.util.DateUtils;
import com.parking.util.ExportCsvUtil;
import com.parking.util.ExportPdfUtil;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Statistiques et export CSV (§4.6 CDC).
 */
public class StatistiquesController {

    private static final Logger logger = LoggerFactory.getLogger(StatistiquesController.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @FXML private Label entreesJourLabel;
    @FXML private Label entreesSemaineLabel;
    @FXML private Label entreesMoisLabel;
    @FXML private Label entreesAnneeLabel;
    @FXML private Label dureeMoyenneLabel;
    @FXML private Label recetteTotaleLabel;
    @FXML private Label statusLabel;
    @FXML private LineChart<String, Number> recettesChart;
    @FXML private BarChart<String, Number> heuresChart;

    private final StatsService statsService = new StatsService();

    @FXML
    private void initialize() {
        statusLabel.setText("");
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onExportCsv() {
        clearStatus();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les tickets en CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        String defaultName = "export_tickets_" + LocalDateTime.now().format(FILE_TS) + ".csv";
        chooser.setInitialFileName(defaultName);
        chooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Documents"));

        File file = chooser.showSaveDialog(SceneNavigator.getPrimaryStage());
        if (file == null) {
            return;
        }

        try {
            Path path = file.toPath();
            ExportCsvUtil.exportTickets(path, statsService.getTicketsPourExport());
            showSuccess("Export réussi : " + path);
        } catch (IOException e) {
            logger.error("Export CSV échoué : {}", e.getMessage());
            showError("Export impossible : " + e.getMessage());
        }
    }

    @FXML
    private void onExportPdf() {
        clearStatus();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les tickets en PDF");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );

        String defaultName =
                "rapport_tickets_" +
                        LocalDateTime.now().format(FILE_TS) +
                        ".pdf";

        chooser.setInitialFileName(defaultName);

        chooser.setInitialDirectory(
                new File(System.getProperty("user.home") + "\\Documents")
        );

        File file = chooser.showSaveDialog(
                SceneNavigator.getPrimaryStage()
        );

        if (file == null) {
            return;
        }

        try {
            Path path = file.toPath();

            ExportPdfUtil.exportTickets(
                    path,
                    statsService.getTicketsPourExport()
            );

            showSuccess("Export PDF réussi : " + path);

        } catch (IOException e) {

            logger.error(
                    "Export PDF échoué : {}",
                    e.getMessage(),
                    e
            );

            showError(
                    "Export PDF impossible : " +
                            e.getMessage()
            );
        }
    }

    private void refresh() {
        clearStatus();
        try {
            StatsSnapshot snapshot = statsService.getSnapshot();
            entreesJourLabel.setText(String.valueOf(snapshot.getEntreesJour()));
            entreesSemaineLabel.setText(String.valueOf(snapshot.getEntreesSemaine()));
            entreesMoisLabel.setText(String.valueOf(snapshot.getEntreesMois()));
            entreesAnneeLabel.setText(String.valueOf(snapshot.getEntreesAnnee()));
            dureeMoyenneLabel.setText(DateUtils.formatDuree(
                    Math.round(snapshot.getDureeMoyenneMinutes())));
            recetteTotaleLabel.setText(formatEuros(snapshot.getRecetteTotale()));

            populateRecettesChart(snapshot);
            populateHeuresChart(snapshot);
        } catch (DatabaseException e) {
            SceneNavigator.showDatabaseError(e);
            showError("Impossible de charger les statistiques.");
        }
    }

    private void populateRecettesChart(StatsSnapshot snapshot) {
        recettesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Recettes (€)");
        for (StatsSnapshot.RecetteJour jour : snapshot.getRecettesParJour()) {
            series.getData().add(new XYChart.Data<>(jour.jour(), jour.montant()));
        }
        recettesChart.getData().add(series);
    }

    private void populateHeuresChart(StatsSnapshot snapshot) {
        heuresChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Entrées");
        snapshot.getEntreesParHeure().forEach((heure, count) ->
                series.getData().add(new XYChart.Data<>(String.format("%02dh", heure), count)));
        heuresChart.getData().add(series);
    }

    private static String formatEuros(BigDecimal montant) {
        return montant != null ? String.format("%.2f €", montant) : "0,00 €";
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
