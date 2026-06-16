package com.parking.controller;

import com.parking.config.DatabaseConfig;
import com.parking.exception.DatabaseException;
import com.parking.model.Utilisateur;
import com.parking.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Contrôleur de l'écran de connexion (§4.1 CDC — authentification BCrypt).
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        statusLabel.setText("");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> onLoginClicked());

        Platform.runLater(() -> loginField.requestFocus());
    }

    @FXML
    private void onLoginClicked() {
        clearStatusStyle();
        setFormDisabled(true);

        String login = loginField.getText().trim();
        String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir votre identifiant et votre mot de passe.");
            setFormDisabled(false);
            return;
        }

        try {
            Connection con = DatabaseConfig.getInstance().getConnection();

            if (con != null) {
                System.out.println("Connexion établie");
            } else {
                System.out.println("Connexion non établie");
            }


            authService.login(login, password).ifPresentOrElse(
                    this::onLoginSuccess,
                    () -> showError("Identifiants incorrects ou compte inactif.")
            );


        } catch (DatabaseException e) {
            logger.error("Erreur JDBC lors de la connexion : {}", e.getMessage());
            SceneNavigator.showDatabaseError(e);
            showError("Connexion à la base de données impossible.");
        } catch (RuntimeException e) {
            logger.error("Erreur inattendue lors de la connexion : {}", e.getMessage(), e);
            showError("Erreur inattendue : " + e.getMessage());
        } finally {
            if (!authService.isAuthenticated()) {
                setFormDisabled(false);
            }
        }
    }

    private void onLoginSuccess(Utilisateur utilisateur) {
        logger.info("Redirection vers l'écran principal — utilisateur {}", utilisateur.getLogin());
        statusLabel.setText("Connexion réussie. Chargement…");
        statusLabel.getStyleClass().add("status-success");
        passwordField.clear();
        SceneNavigator.showMain();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().add("status-error");
    }

    private void clearStatusStyle() {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.setText("");
    }

    private void setFormDisabled(boolean disabled) {
        loginField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }
}
