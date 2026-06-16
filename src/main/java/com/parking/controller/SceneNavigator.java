package com.parking.controller;

import com.parking.config.AppConfig;
import com.parking.exception.DatabaseException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Navigation centralisée entre les écrans JavaFX (login ↔ application principale).
 */
public final class SceneNavigator {

    private static final Logger logger = LoggerFactory.getLogger(SceneNavigator.class);

    private static Stage primaryStage;
    private static AppConfig appConfig;

    private SceneNavigator() {
    }

    public static void init(Stage stage, AppConfig config) {
        primaryStage = Objects.requireNonNull(stage, "primaryStage");
        appConfig = Objects.requireNonNull(config, "appConfig");
    }

    public static void showLogin() {
        ensureInitialized();
        try {
            Parent root = loadFxml("/fxml/Login.fxml");
            applyScene(root);
            logger.debug("Écran de connexion affiché");
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger Login.fxml", e);
        }
    }

    public static void showMain() {
        ensureInitialized();
        try {
            Parent root = loadFxml("/fxml/Main.fxml");
            applyScene(root);
            logger.debug("Écran principal affiché");
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger Main.fxml", e);
        }
    }

    public static Stage getPrimaryStage() {
        ensureInitialized();
        return primaryStage;
    }

    public static AppConfig getAppConfig() {
        ensureInitialized();
        return appConfig;
    }

    private static void applyScene(Parent root) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        URL cssUrl = SceneNavigator.class.getResource("/css/main.css");
        if (cssUrl != null && !scene.getStylesheets().contains(cssUrl.toExternalForm())) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle(appConfig.getUiTitre());
    }

    private static Parent loadFxml(String resourcePath) throws IOException {
        URL fxmlUrl = SceneNavigator.class.getResource(resourcePath);
        if (fxmlUrl == null) {
            throw new IOException("FXML introuvable : " + resourcePath);
        }
        return new FXMLLoader(fxmlUrl).load();
    }

    private static void ensureInitialized() {
        if (primaryStage == null || appConfig == null) {
            throw new IllegalStateException("SceneNavigator non initialisé — appelez init() au démarrage");
        }
    }

    /**
     * Affiche un message d'erreur réseau / base de données à l'utilisateur.
     */
    public static void showDatabaseError(DatabaseException exception) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur base de données");
        alert.setHeaderText("Impossible de contacter MySQL");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
