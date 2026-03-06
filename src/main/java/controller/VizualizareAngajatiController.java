package controller;

import database.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Controller Vizualizare Angajati
 * Pop-up simplu ==> Arata cine lucreaza la un eveniment specific.
 */
public class VizualizareAngajatiController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> angajatiListView;

    private ObservableList<String> listaAngajati = FXCollections.observableArrayList();

    // Init ==> Primeste ID eveniment si incarca lista
    public void initData(int eventId, String eventName) {
        titleLabel.setText("Personal Alocat - " + eventName);

        // query 21 SQL: JOIN Angajati + Link Table ==> Numele si Rolul celor alocati la evenimentul X
        String sql = "SELECT A.Nume, A.Prenume, EA.Rol " +
                "FROM Angajati AS A " +
                "JOIN Evenimente_Angajati AS EA ON A.IDAngajat = EA.IDAngajat " +
                "WHERE EA.IDEveniment = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Formatare: "Ion Popescu (Ospatar)"
                String numeComplet = rs.getString("Nume") + " " + rs.getString("Prenume");
                String rol = rs.getString("Rol");
                listaAngajati.add(numeComplet + " (" + rol + ")");
            }

            // Check gol ==> Mesaj default
            if (listaAngajati.isEmpty()) {
                listaAngajati.add("Niciun angajat alocat.");
            }

            angajatiListView.setItems(listaAngajati);

        } catch (SQLException e) {
            e.printStackTrace();
            listaAngajati.add("Eroare la incarcarea datelor.");
            angajatiListView.setItems(listaAngajati);
        }
    }

    // Close window
    @FXML
    private void handleClose() {
        Stage stage = (Stage) angajatiListView.getScene().getWindow();
        stage.close();
    }
}