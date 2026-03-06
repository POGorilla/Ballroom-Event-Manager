package controller;

import database.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/*
 * Controller Layout Principal
 * Gestioneaza navigarea intre pagini fara sa inchida aplicatia
 */
public class MainLayoutController {

    @FXML
    private StackPane contentArea; // Aici se incarca paginile
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnEvenimente;
    @FXML
    private Button btnLogout;

    @FXML
    public void initialize() {
        loadPage("/Dashboard/Dashboard.fxml");
    }

    // Navigare ==> Dashboard
    @FXML
    private void handleDashboardClick(ActionEvent event) {
        loadPage("/Dashboard/Dashboard.fxml");
    }

    // Navigare ==> Rapoarte
    @FXML
    private void handleOpenReports(ActionEvent event) {
        loadPage("/Formulare/Rapoarte.fxml");
    }

    // Click Admin Panel ==> Cere parola din nou pt securitate extra
    @FXML
    private void handleAdminPanel(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Verificare Securitate");
        dialog.setHeaderText("Acces Restrictionat: Admin Panel");
        dialog.setContentText("Introduceti parola dvs. pentru confirmare:");

        // Configurare Dialog Custom cu PasswordField (sa nu se vada caracterele)
        Dialog<String> passDialog = new Dialog<>();
        passDialog.setTitle("Securitate");
        passDialog.setHeaderText("Confirmare identitate");
        ButtonType loginButtonType = new ButtonType("Acces", ButtonBar.ButtonData.OK_DONE);
        passDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Parola curenta");
        VBox vbox = new VBox(new Label("Parola:"), passwordField);
        vbox.setSpacing(10);
        passDialog.getDialogPane().setContent(vbox);

        // Convertire rezultat buton ==> text parola
        passDialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        // Afisare si asteptare input
        Optional<String> result = passDialog.showAndWait();

        // Verificare logica
        result.ifPresent(password -> {
            if (verifyPassword(password)) {
                loadPage("/Formulare/AdminPanel.fxml"); // OK ==> Deschide
            } else {
                // Eroare ==> Alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Eroare");
                alert.setContentText("Parola incorecta! Acces respins.");
                alert.showAndWait();
            }
        });
    }

    // Verifica parola curenta cu cea din DB folosind BCrypt
    private boolean verifyPassword(String inputPass) {
        UserSession session = UserSession.getInstance();
        if (session == null) return false;

        // SQL: SELECT ==> Ia hash-ul parolei userului curent
        try (Connection conn = DatabaseManager.getConnection();
             //query 19
             PreparedStatement pstmt = conn.prepareStatement("SELECT ParolaHash FROM Utilizatori WHERE IDUtilizator = ?")) {

            pstmt.setInt(1, session.getUserId());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hash = rs.getString("ParolaHash");
                // BCrypt: Compara input vs Hash
                return org.mindrot.jbcrypt.BCrypt.checkpw(inputPass, hash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Navigare ==> Evenimente
    @FXML
    private void handleEvenimenteClick(ActionEvent event) {
        loadPage("/Evenimente/Evenimente.fxml");
    }

    // Logout ==> Incarca scena de Login si schimba fereastra
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login/Login.fxml"));
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);

            Stage currentStage = (Stage) btnLogout.getScene().getWindow();
            String css = getClass().getResource("/Login/style.css").toExternalForm();
            loginScene.getStylesheets().add(css);

            currentStage.setScene(loginScene);
            currentStage.setTitle("Ballroom Event Management - Login");
            currentStage.setResizable(false);
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // schimba FXML-ul din contentArea (partea dreapta)
    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Eroare la incarcarea paginii: " + fxmlFile);
        }
    }
}