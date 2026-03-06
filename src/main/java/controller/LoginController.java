package controller;

import database.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/*
 * Controller Login
 * Gestioneaza autentificarea userilor
 */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    // Click Login ==> Verificare + Schimbare Scena
    @FXML
    protected void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Check DB
        if (authenticate(username, password)) {
            try {
                // Auth OK ==> Incarca MainLayout
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login/MainLayout.fxml"));
                Parent mainLayoutRoot = loader.load();
                Scene mainScene = new Scene(mainLayoutRoot);

                // Setup scena noua + CSS
                Stage currentStage = (Stage) loginButton.getScene().getWindow();
                String css = getClass().getResource("/Login/style.css").toExternalForm();
                mainScene.getStylesheets().add(css);

                currentStage.setScene(mainScene);
                currentStage.setTitle("Ballroom - Panou Principal");
                currentStage.setResizable(true);
                currentStage.centerOnScreen();

            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Eroare la incarcarea aplicatiei.");
                errorLabel.setVisible(true);
            }
        } else {
            // Auth Fail ==> Mesaj eroare
            errorLabel.setText("Utilizator sau parola incorecta.");
            errorLabel.setVisible(true);
        }
    }

    // Verifica user/parola in DB
    private boolean authenticate(String username, String password) {
        // query 18
        String sql = "SELECT IDUtilizator, ParolaHash FROM Utilizatori WHERE Username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int idUtilizator = rs.getInt("IDUtilizator");
                    String parolaStocataHash = rs.getString("ParolaHash");

                    // BCrypt ==> Verifica daca parola introdusa bate cu hash-ul
                    if (BCrypt.checkpw(password, parolaStocataHash)) {
                        utils.UserSession.setSession(idUtilizator, username); // Set Session
                        logAccess(idUtilizator, username, "Succes"); // Log DB
                        return true;
                    } else {
                        logAccess(idUtilizator, username, "Esuat"); // Pass gresita
                        return false;
                    }
                } else {
                    logAccess(null, username, "Esuat"); // User inexistent
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Scrie in tabelul LogAcces cine a incercat sa intre
    private void logAccess(Integer idUtilizator, String usernameIncercat, String status) {
        String sqlLog = "INSERT INTO LogAcces (IDUtilizator, UsernameIncercat, StatusLogin) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlLog)) {

            if (idUtilizator != null) {
                pstmt.setInt(1, idUtilizator);
            } else {
                pstmt.setNull(1, Types.INTEGER); // User inexistent ==> ID NULL
            }
            pstmt.setString(2, usernameIncercat);
            pstmt.setString(3, status);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Eroare la scrierea in LogAcces!");
        }
    }
}