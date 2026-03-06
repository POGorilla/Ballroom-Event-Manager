package controller;

import database.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.MainApp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Controller Lista Contacte
 * Fereastra pop-up cu lista scrollabila + butoane actiune (Suna/Email).
 */
public class ContactListController {

    @FXML
    private Label titleLabel;
    @FXML
    private ListView<ContactItem> contactListView;

    private ObservableList<ContactItem> contactList = FXCollections.observableArrayList();

    /*
     * Model simplu (Date in memorie)
     * Structura de date pt un rand din lista
     */
    private static class ContactItem {
        String nume;
        String rol;
        String telefon;
        String email;

        public ContactItem(String nume, String rol, String telefon, String email) {
            this.nume = nume;
            // Rol null ==> Client
            this.rol = (rol == null || rol.isEmpty()) ? "Client" : rol;
            this.telefon = (telefon == null) ? "" : telefon;
            this.email = (email == null) ? "" : email;
        }

        public String getNume() { return nume; }
        public String getRol() { return rol; }
        public String getTelefon() { return telefon; }
        public String getEmail() { return email; }
    }

    /*Start ==> Incarca datele in functie de tip (clienti vs supervizori)*/
    public void initData(String contactType) {
        contactListView.setItems(contactList);
        contactListView.setCellFactory(lv -> new CustomContactCell());

        if ("clienti".equals(contactType)) {
            titleLabel.setText("Contact Clienti");
            loadClienti();
        } else if ("supervizori".equals(contactType)) {
            titleLabel.setText("Contact Supervizori (Roluri)");
            loadSupervizori();
        }
    }

    // query 4 SQL: SELECT ==> Iau toti clientii ordonati alfabetic
    private void loadClienti() {
        String sql = "SELECT Nume, Prenume, Telefon, Email FROM Clienti ORDER BY Nume, Prenume";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                contactList.add(new ContactItem(
                        rs.getString("Nume") + " " + rs.getString("Prenume"),
                        null,
                        rs.getString("Telefon"),
                        rs.getString("Email")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Iau doar sefi SAU pe cei fara sef (Manager General)
    private void loadSupervizori() {
        // query 23
        String sql = "SELECT Nume, Prenume, Telefon, Email, Functie FROM Angajati " +
                "WHERE IDAngajat IN (SELECT DISTINCT IDSupervizor FROM Angajati WHERE IDSupervizor IS NOT NULL) " +
                "OR IDSupervizor IS NULL " +
                "ORDER BY Nume, Prenume";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                contactList.add(new ContactItem(
                        rs.getString("Nume") + " " + rs.getString("Prenume"),
                        rs.getString("Functie"),
                        rs.getString("Telefon"),
                        rs.getString("Email")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Buton Inchide ==> Close window
    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    private class CustomContactCell extends ListCell<ContactItem> {
        private VBox content = new VBox(5);
        private Label lblNume = new Label();
        private Label lblRol = new Label();
        private HBox buttonBox = new HBox(10);
        private Button btnEmail = new Button("Email");
        private Button btnTelefon = new Button("Suna");

        public CustomContactCell() {
            super();
            lblNume.setStyle("-fx-font-weight: bold; -fx-text-fill: #E0E0E0;");
            lblRol.setStyle("-fx-font-style: italic; -fx-text-fill: #AAAAAA;");

            btnEmail.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");
            btnTelefon.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");

            buttonBox.getChildren().addAll(btnEmail, btnTelefon);
            content.getChildren().addAll(lblNume, lblRol, buttonBox);
        }

        /* Update UI automat cand dai scroll */
        @Override
        protected void updateItem(ContactItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                lblNume.setText(item.getNume());
                lblRol.setText(item.getRol());

                // Enable/Disable buton daca nu are email
                btnEmail.setDisable(item.getEmail().isEmpty());
                btnEmail.setOnAction(e -> {
                    System.out.println("Se deschide email: " + item.getEmail());
                    // Deschide app de mail default
                    MainApp.getStaticHostServices().showDocument("mailto:" + item.getEmail());
                });

                // Enable/Disable buton daca nu are telefon
                btnTelefon.setDisable(item.getTelefon().isEmpty());
                btnTelefon.setOnAction(e -> {
                    // Curata nr de caractere non-numerice ==> apelare
                    String telefon = item.getTelefon().replaceAll("[^\\d+]", "");
                    System.out.println("Se apeleaza: " + telefon);
                    MainApp.getStaticHostServices().showDocument("tel:" + telefon);
                });

                setGraphic(content);
            }
        }
    }
}