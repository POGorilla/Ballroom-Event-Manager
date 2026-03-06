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
 * Controller Vizualizare Servicii
 * Pop-up simplu ==> Lista servicii alocate + Cost total pt un eveniment
 */
public class VizualizareServiciiController {

    @FXML private Label titleLabel;
    @FXML private ListView<String> serviciiListView;
    @FXML private Label totalCostLabel;

    private ObservableList<String> listaServicii = FXCollections.observableArrayList();
    private double costTotal = 0.0;

    // Init ==> Primeste ID eveniment si incarca serviciile + calculeaza costul
    public void initData(int eventId, String eventName) {
        titleLabel.setText("Servicii - " + eventName);

        // query 22 SQL: JOIN Servicii + Link Table ==> Denumirea si pretul serviciilor pt evenimentul X
        String sql = "SELECT S.Denumire, S.Pret, ES.Cantitate " +
                "FROM Servicii AS S " +
                "JOIN Evenimente_Servicii AS ES ON S.IDServiciu = ES.IDServiciu " +
                "WHERE ES.IDEveniment = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            // Loop prin rezultate ==> Adaugare in lista + Calcul suma
            while (rs.next()) {
                String denumire = rs.getString("Denumire");
                double pret = rs.getDouble("Pret");
                int cantitate = rs.getInt("Cantitate");
                if (cantitate <= 0) cantitate = 1;

                listaServicii.add(denumire + " (" + String.format("%.2f", pret) + " RON x " + cantitate + " buc)");
                costTotal += pret * cantitate;
            }

            // Check gol ==> Mesaj default
            if (listaServicii.isEmpty()) {
                listaServicii.add("Niciun serviciu alocat.");
            }

            // Update UI ==> Setare lista si text cost total
            serviciiListView.setItems(listaServicii);
            totalCostLabel.setText(String.format("%.2f RON", costTotal));

        } catch (SQLException e) {
            e.printStackTrace();
            listaServicii.add("Eroare la incarcarea datelor.");
            serviciiListView.setItems(listaServicii);
        }
    }

    // Close window
    @FXML
    private void handleClose() {
        Stage stage = (Stage) serviciiListView.getScene().getWindow();
        stage.close();
    }
}