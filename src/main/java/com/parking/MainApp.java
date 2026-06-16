package com.parking;

import com.parking.config.AppConfig;
import com.parking.config.DatabaseConfig;
import com.parking.controller.SceneNavigator;
import com.parking.exception.DatabaseException;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée de l'application Parking Moto.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Charger la configuration depuis {@code config.properties}</li>
 *   <li>Initialiser le pool de connexions JDBC</li>
 *   <li>Afficher l'écran de connexion (Login)</li>
 *   <li>Fermer proprement les ressources à l'arrêt</li>
 * </ul>
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {
        logger.info("=== Démarrage de l'application Parking Moto v1.0.0 ===");

        try {
            AppConfig config = AppConfig.getInstance();
            logger.info("Configuration chargée — parking: {}, places: {}",
                    config.getParkingNom(), config.getNombrePlaces());

            try {
                DatabaseConfig.getInstance().testConnection();
                logger.info("Connexion MySQL vérifiée avec succès");
            } catch (DatabaseException e) {
                logger.warn("MySQL indisponible — l'écran de connexion s'affiche quand même : {}",
                        e.getMessage());
            }

            SceneNavigator.init(primaryStage, config);

            primaryStage.setTitle(config.getUiTitre());
            primaryStage.setWidth(config.getUiLargeur());
            primaryStage.setHeight(config.getUiHauteur());
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(600);

            SceneNavigator.showLogin();
            primaryStage.show();

            logger.info("Interface graphique initialisée — {}x{}",
                    config.getUiLargeur(), config.getUiHauteur());

        } catch (Exception e) {
            logger.error("Erreur fatale inattendue : {}", e.getMessage(), e);
            showFatalError("Erreur inattendue", e.getMessage());
        }
    }

    @Override
    public void stop() {
        logger.info("Arrêt de l'application — fermeture des ressources...");
        try {
            DatabaseConfig.getInstance().closePool();
            logger.info("Pool de connexions JDBC fermé proprement");
        } catch (Exception e) {
            logger.warn("Avertissement lors de la fermeture du pool : {}", e.getMessage());
        }
        logger.info("=== Application Parking Moto arrêtée ===");
    }

    private void showFatalError(String titre, String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(titre);
            alert.setHeaderText("L'application ne peut pas démarrer");
            alert.setContentText(message + "\n\nVérifiez config.properties et la connexion MySQL.");
            alert.showAndWait();
            javafx.application.Platform.exit();
        });
    }

    public static void main(String[] args) {
        logger.debug("Lancement via main() — transmission à JavaFX Application.launch()");
        launch(args);
    }
}
