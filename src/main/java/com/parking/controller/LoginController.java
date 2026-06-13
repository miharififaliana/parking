package com.parking.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contrôleur de l'écran de connexion (Phase 1 — squelette sans logique métier).
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        statusLabel.setText("");
        loginButton.setOnAction(event -> onLoginClicked());
    }

    @FXML
    private void onLoginClicked() {
        String login = loginField.getText().trim();
        logger.debug("Tentative de connexion pour l'utilisateur : {}", login.isEmpty() ? "(vide)" : login);
        statusLabel.setText("Authentification — disponible en Phase 3");
    }
}
