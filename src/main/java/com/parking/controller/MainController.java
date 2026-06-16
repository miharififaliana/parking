package com.parking.controller;

import com.parking.model.Utilisateur;
import com.parking.service.AuthService;
import com.parking.service.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.Parent;

/**
 * Contrôleur du conteneur principal — menu latéral et zone de contenu (§7 CDC).
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Label userLabel;
    @FXML private Label parkingLabel;
    @FXML private StackPane contentArea;
    @FXML private Button dashboardNavButton;
    @FXML private Button entreeNavButton;
    @FXML private Button sortieNavButton;
    @FXML private Button placesNavButton;
    @FXML private Button statsNavButton;
    @FXML private Button logoutButton;

    private final AuthService authService = new AuthService();
    private final Map<Button, String> navFxml = new HashMap<>();
    private DashboardController dashboardController;
    private Button activeNavButton;

    @FXML
    private void initialize() {
        if (!authService.isAuthenticated()) {
            logger.warn("Accès à Main sans session — redirection login");
            SceneNavigator.showLogin();
            return;
        }

        navFxml.put(dashboardNavButton, "/fxml/Dashboard.fxml");
        navFxml.put(entreeNavButton, "/fxml/Entree.fxml");
        navFxml.put(sortieNavButton, "/fxml/Sortie.fxml");
        navFxml.put(placesNavButton, "/fxml/Places.fxml");
        navFxml.put(statsNavButton, "/fxml/Statistiques.fxml");

        authService.getUtilisateurConnecte().ifPresent(this::afficherUtilisateur);
        parkingLabel.setText(SceneNavigator.getAppConfig().getParkingNom());
        logoutButton.setOnAction(e -> onLogoutClicked());

        showDashboard();
    }

    @FXML private void onNavEntree() { loadView(entreeNavButton); }
    @FXML private void onNavSortie() { loadView(sortieNavButton); }
    @FXML private void onNavPlaces() { loadView(placesNavButton); }
    @FXML private void onNavStatistiques() { loadView(statsNavButton); }

    @FXML
    private void showDashboard() {
        loadView(dashboardNavButton);
    }

    private void loadView(Button navButton) {
        stopActiveDashboardRefresh();
        String fxml = navFxml.get(navButton);
        if (fxml == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            contentArea.getChildren().setAll(Collections.singleton(loader.load()));
            if (navButton == dashboardNavButton) {
                dashboardController = loader.getController();
                dashboardController.startRefreshing();
            }
            setActiveNav(navButton);
        } catch (IOException e) {
            logger.error("Impossible de charger {} : {}", fxml, e.getMessage(), e);
        }
    }

    private void onLogoutClicked() {
        stopActiveDashboardRefresh();
        authService.logout();
        SceneNavigator.showLogin();
    }

    private void afficherUtilisateur(Utilisateur utilisateur) {
        userLabel.setText(utilisateur.getNom() + " (" + utilisateur.getRole() + ")");
    }

    private void setActiveNav(Button activeButton) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-active");
        }
        activeNavButton = activeButton;
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().add("nav-active");
        }
    }

    private void stopActiveDashboardRefresh() {
        if (dashboardController != null) {
            dashboardController.stopRefreshing();
            dashboardController = null;
        }
    }

    public void onSceneShown() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            SceneNavigator.showLogin();
        }
    }
}
