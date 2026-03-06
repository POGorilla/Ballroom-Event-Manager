package controller;

import database.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*
 * Controller Formular Client (Add / Edit)
 * Fereastra pop-up pt salvare date client
 */
public class FormularClientController {
    @FXML private Label titleLabel;
    @FXML private TextField numeField;
    @FXML private TextField prenumeField;
    @FXML private TextField telefonField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> tipClientComboBox;
    @FXML private Button cancelButton;

    private Integer currentClientId = null; // null ==> Add, ID ==> Edit

    // Init ==> Configurare dropdown
    @FXML
    public void initialize() {
        tipClientComboBox.setItems(FXCollections.observableArrayList("Persoana fizica", "Firma"));
        tipClientComboBox.setValue("Persoana fizica");
    }

    // Primire date de la fereastra parinte
    // Daca ID != null ==> Mod Editare
    public void initData(Integer clientId) {
        this.currentClientId = clientId;
        if (clientId != null) {
            titleLabel.setText("Modifica Client");
            loadClientData();
        }
    }

    // query 10 SQL: SELECT ==> Pre-completare campuri (pt Edit)
    private void loadClientData() {
        String sql = "SELECT * FROM Clienti WHERE IDClient = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentClientId);
            var rs = pstmt.executeQuery();

            if (rs.next()) {
                numeField.setText(rs.getString("Nume"));
                prenumeField.setText(rs.getString("Prenume"));
                telefonField.setText(rs.getString("Telefon"));
                emailField.setText(rs.getString("Email"));
                tipClientComboBox.setValue(rs.getString("Tip_Client"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Buton Save ==> Validare + Insert/Update DB
    @FXML
    private void handleSave() {
        String nume = numeField.getText().trim();
        String prenume = prenumeField.getText().trim();
        String telefon = telefonField.getText().trim();
        String email = emailField.getText().trim();
        String tip = tipClientComboBox.getValue();

        // Validare nume + tel (exista sau nu)
        if (nume.isEmpty() || telefon.isEmpty()) {
            showError("Numele si Telefonul sunt obligatorii!");
            return;
        }

        // Validare format telefon (07xxxxxxxx)
        if (!telefon.matches("^07\\d{8}$")) {
            showError("Numar de telefon invalid! Trebuie sa inceapa cu '07' si sa aiba 10 cifre.");
            return;
        }

        // Validare format email (daca e completat)
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format Email invalid! Exemplu: client@email.com");
            return;
        }

        String sql;
        // Check ID ==> Decide Insert sau Update
        if (currentClientId == null) {
            // SQL: INSERT ==> Client nou
            sql = "INSERT INTO Clienti (Nume, Prenume, Telefon, Email, Tip_Client) VALUES (?, ?, ?, ?, ?)";
        } else {
            // SQL: UPDATE ==> Modificare client existent
            sql = "UPDATE Clienti SET Nume=?, Prenume=?, Telefon=?, Email=?, Tip_Client=? WHERE IDClient=?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nume);
            pstmt.setString(2, prenume);
            pstmt.setString(3, telefon);
            pstmt.setString(4, email);
            pstmt.setString(5, tip);

            if (currentClientId != null) {
                pstmt.setInt(6, currentClientId);
            }

            pstmt.executeUpdate();

            showInfo("Client salvat cu succes!");
            closeWindow();

        } catch (SQLException e) {
            showError("Eroare la salvarea in baza de date: " + e.getMessage());
        }
    }

    // Cancel ==> Close window
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // Pop-up Eroare
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare Validare");
        alert.setHeaderText("Date incorecte");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Pop-up Succes
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}