package com.parking;

import com.parking.config.AppConfig;
import com.parking.config.DatabaseConfig;
import com.parking.exception.DatabaseException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

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
 *
 * @author  Système de Gestion de Parking Moto
 * @version 1.0.0
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    /**
     * Point d'entrée JavaFX — appelé après {@link #main(String[])}.
     * Initialise la scène principale et affiche l'écran de login.
     *
     * @param primaryStage la fenêtre principale fournie par JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        logger.info("=== Démarrage de l'application Parking Moto v1.0.0 ===");

        try {
            // 1. Charger la configuration applicative (config.properties)
            AppConfig config = AppConfig.getInstance();
            logger.info("Configuration chargée — parking: {}, places: {}",
                    config.getParkingNom(), config.getNombrePlaces());

            // 2. Tester la connexion MySQL (avertissement seulement en Phase 1)
            try {
                DatabaseConfig.getInstance().testConnection();
                logger.info("Connexion MySQL vérifiée avec succès");
            } catch (DatabaseException e) {
                logger.warn("MySQL indisponible — l'écran de connexion s'affiche quand même : {}",
                        e.getMessage());
            }

            // 3. Charger le FXML de l'écran de connexion
            URL fxmlUrl = getClass().getResource("/fxml/Login.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Fichier FXML introuvable : /fxml/Login.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load());

            // 4. Appliquer la feuille de style globale
            URL cssUrl = getClass().getResource("/css/main.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            // 5. Configurer et afficher la fenêtre principale
            primaryStage.setTitle(config.getUiTitre());
            primaryStage.setWidth(config.getUiLargeur());
            primaryStage.setHeight(config.getUiHauteur());
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(600);
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Interface graphique initialisée — {}x{}",
                    config.getUiLargeur(), config.getUiHauteur());

        } catch (IOException e) {
            logger.error("Erreur fatale lors du chargement de l'interface : {}", e.getMessage(), e);
            showFatalError("Erreur de démarrage", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur fatale inattendue : {}", e.getMessage(), e);
            showFatalError("Erreur inattendue", e.getMessage());
        }
    }

    /**
     * Nettoyage des ressources lors de la fermeture de l'application.
     * Ferme proprement le pool de connexions JDBC.
     */
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

    /**
     * Affiche une boîte de dialogue d'erreur fatale et termine l'application.
     *
     * @param titre   titre de la boîte de dialogue
     * @param message message d'erreur à afficher
     */
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

    /**
     * Point d'entrée principal Java — nécessaire pour lancer un JAR exécutable
     * avec JavaFX (contournement de la restriction du launcher JavaFX).
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        logger.debug("Lancement via main() — transmission à JavaFX Application.launch()");
        launch(args);
    }
}