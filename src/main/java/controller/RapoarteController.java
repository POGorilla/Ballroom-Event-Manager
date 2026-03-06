package controller;

import database.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;

/*
 * Controller Rapoarte
 * Genereaza tabele dinamice direct din SQL
 */
public class RapoarteController {

    @FXML private TableView<ObservableList<String>> dynamicTable;
    @FXML private Label reportTitleLabel;
    @FXML private ComboBox<Integer> yearCombo;

    @FXML
    public void initialize() {
        // Umple combo cu ani (din DB + fallback in jurul anului curent)
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int current = LocalDate.now().getYear();
        years.add(current);

        // Incercam sa luam anii existenti din DB
        try (Connection conn = DatabaseManager.getConnection();
             // query 20
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT DISTINCT YEAR(Data_Inceput) AS y FROM Evenimente ORDER BY y DESC");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int y = rs.getInt(1);
                if (!years.contains(y)) years.add(y);
            }
        } catch (Exception ignored) {
            // daca nu exista tabela / nu sunt evenimente inca, mergem pe fallback
        }

        // Fallback: ani in jurul anului curent
        for (int y = current - 4; y <= current + 1; y++) {
            if (!years.contains(y)) years.add(y);
        }

        FXCollections.sort(years);
        yearCombo.setItems(years);
        yearCombo.setValue(current);
    }

    private int selectedYear() {
        Integer y = (yearCombo != null) ? yearCombo.getValue() : null;
        return (y != null) ? y : LocalDate.now().getYear();
    }

    // 1) Top clienti in anul selectat, cu HAVING peste media evenimentelor din acel an
    @FXML
    private void showTopClients() {
        int year = selectedYear();
        reportTitleLabel.setText("Top Clienti (An " + year + ")");

        // query 25
        String sql =
                "SELECT C.Nume, C.Prenume, SUM(E.Pret_Total) AS 'Total Cheltuit (RON)' " +
                        "FROM Clienti C " +
                        "JOIN Evenimente E ON C.IDClient = E.IDClient " +
                        "WHERE YEAR(E.Data_Inceput) = ? " +
                        "GROUP BY C.IDClient, C.Nume, C.Prenume " +
                        "HAVING SUM(E.Pret_Total) > ( " +
                        "   SELECT AVG(Pret_Total) FROM Evenimente WHERE YEAR(Data_Inceput) = ? " +
                        ") " +
                        "ORDER BY SUM(E.Pret_Total) DESC";

        populateTable(sql, year, year);
    }

    // 2) Angajati fara activitate in anul selectat
    @FXML
    private void showIdleEmployees() {
        int year = selectedYear();
        reportTitleLabel.setText("Angajati fara activitate (An " + year + ")");

        // query 26
        String sql =
                "SELECT A.Nume, A.Prenume, A.Functie " +
                        "FROM Angajati A " +
                        "WHERE NOT EXISTS ( " +
                        "   SELECT 1 " +
                        "   FROM Evenimente_Angajati EA " +
                        "   JOIN Evenimente E ON E.IDEveniment = EA.IDEveniment " +
                        "   WHERE EA.IDAngajat = A.IDAngajat " +
                        "     AND YEAR(E.Data_Inceput) = ? " +
                        ") " +
                        "ORDER BY A.Nume, A.Prenume";

        populateTable(sql, year);
    }

    // 3) Venituri pe sala, dar afiseaza doar salile peste media veniturilor/sala
    @FXML
    private void showRevenuePerRoom() {
        int year = selectedYear();
        reportTitleLabel.setText("Venituri per sala peste medie (An " + year + ")");

        // query 27
        String sql =
                "SELECT S.Denumire AS 'Nume Sala', SUM(E.Pret_Total) AS 'Total Incasari' " +
                        "FROM Sali S " +
                        "JOIN Evenimente E ON S.IDSala = E.IDSala " +
                        "WHERE YEAR(E.Data_Inceput) = ? " +
                        "GROUP BY S.IDSala, S.Denumire " +
                        "HAVING SUM(E.Pret_Total) > ( " +
                        "   SELECT AVG(RoomTotal) FROM ( " +
                        "       SELECT SUM(E2.Pret_Total) AS RoomTotal " +
                        "       FROM Sali S2 " +
                        "       JOIN Evenimente E2 ON S2.IDSala = E2.IDSala " +
                        "       WHERE YEAR(E2.Data_Inceput) = ? " +
                        "       GROUP BY S2.IDSala " +
                        "   ) t " +
                        ") " +
                        "ORDER BY SUM(E.Pret_Total) DESC";

        populateTable(sql, year, year);
    }

    // 4) Servicii mai populare decat media utilizarii
    @FXML
    private void showTopServices() {
        int year = selectedYear();
        reportTitleLabel.setText("Servicii peste medie (An " + year + ")");

        // query 28
        String sql =
                "SELECT S.Denumire AS Serviciu, COUNT(*) AS 'Nr. Utilizari' " +
                        "FROM Servicii S " +
                        "JOIN Evenimente_Servicii ES ON S.IDServiciu = ES.IDServiciu " +
                        "JOIN Evenimente E ON E.IDEveniment = ES.IDEveniment " +
                        "WHERE YEAR(E.Data_Inceput) = ? " +
                        "GROUP BY S.IDServiciu, S.Denumire " +
                        "HAVING COUNT(*) > ( " +
                        "   SELECT AVG(cnt) FROM ( " +
                        "       SELECT COUNT(*) AS cnt " +
                        "       FROM Evenimente_Servicii ES3 " +
                        "       JOIN Evenimente E3 ON E3.IDEveniment = ES3.IDEveniment " +
                        "       WHERE YEAR(E3.Data_Inceput) = ? " +
                        "       GROUP BY ES3.IDServiciu " +
                        "   ) x " +
                        ") " +
                        "ORDER BY COUNT(*) DESC";

        populateTable(sql, year, year);
    }

    private void populateTable(String sql, Object... params) {
        dynamicTable.getColumns().clear();
        dynamicTable.getItems().clear();

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 0; i < columnCount; i++) {
                    final int colIndex = i;
                    String colName = metaData.getColumnLabel(i + 1);

                    TableColumn<ObservableList<String>, String> col = new TableColumn<>(colName);
                    col.setCellValueFactory(param ->
                            new SimpleStringProperty(param.getValue().get(colIndex)));
                    dynamicTable.getColumns().add(col);
                }

                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= columnCount; i++) {
                        String value = rs.getString(i);
                        row.add(value != null ? value : "-");
                    }
                    data.add(row);
                }
            }

            dynamicTable.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
            reportTitleLabel.setText("Eroare la generare raport: " + e.getMessage());
        }
    }

    // 5) Angajati cu incarcare peste medie (nr. evenimente/an) - COMPLEX + parametru
    @FXML
    private void showBusyEmployees() {
        int year = selectedYear();
        reportTitleLabel.setText("Angajati cu incarcare peste medie (An " + year + ")");

        // query 29
        String sql =
                "SELECT A.Nume, A.Prenume, COUNT(DISTINCT EA.IDEveniment) AS 'Nr. Evenimente' " +
                        "FROM Angajati A " +
                        "JOIN Evenimente_Angajati EA ON EA.IDAngajat = A.IDAngajat " +
                        "JOIN Evenimente E ON E.IDEveniment = EA.IDEveniment " +
                        "WHERE YEAR(E.Data_Inceput) = ? " +
                        "GROUP BY A.IDAngajat, A.Nume, A.Prenume " +
                        "HAVING COUNT(DISTINCT EA.IDEveniment) > ( " +
                        "   SELECT AVG(cnt) FROM ( " +
                        "       SELECT COUNT(DISTINCT EA2.IDEveniment) AS cnt " +
                        "       FROM Angajati A2 " +
                        "       JOIN Evenimente_Angajati EA2 ON EA2.IDAngajat = A2.IDAngajat " +
                        "       JOIN Evenimente E2 ON E2.IDEveniment = EA2.IDEveniment " +
                        "       WHERE YEAR(E2.Data_Inceput) = ? " +
                        "       GROUP BY A2.IDAngajat " +
                        "   ) t " +
                        ") " +
                        "ORDER BY COUNT(DISTINCT EA.IDEveniment) DESC";

        populateTable(sql, year, year);
    }

    // 6) Clienti recurenti: au eveniment in anul selectat SI in anul precedent - COMPLEX + parametru
    @FXML
    private void showReturningClients() {
        int year = selectedYear();
        int prevYear = year - 1;
        reportTitleLabel.setText("Clienti recurenti (An " + year + " si " + prevYear + ")");

        // query 30
        String sql =
                "SELECT C.Nume, C.Prenume, COUNT(E.IDEveniment) AS 'Evenimente in An', SUM(E.Pret_Total) AS 'Total (RON)' " +
                        "FROM Clienti C " +
                        "JOIN Evenimente E ON E.IDClient = C.IDClient " +
                        "WHERE YEAR(E.Data_Inceput) = ? " +
                        "AND EXISTS ( " +
                        "   SELECT 1 FROM Evenimente Eprev " +
                        "   WHERE Eprev.IDClient = C.IDClient " +
                        "     AND YEAR(Eprev.Data_Inceput) = ? " +
                        ") " +
                        "GROUP BY C.IDClient, C.Nume, C.Prenume " +
                        "ORDER BY SUM(E.Pret_Total) DESC";

        populateTable(sql, year, prevYear);
    }

    // 7) Venit pe servicii peste medie (pe anul selectat) - COMPLEX + parametru
    @FXML
    private void showServiceRevenueAboveAvg() {
        int year = selectedYear();
        reportTitleLabel.setText("Servicii cu venit peste medie (An " + year + ")");

        // query 31
        String sql =
                "SELECT S.Denumire AS 'Serviciu', SUM(S.Pret * ES.Cantitate) AS 'Venit (RON)' " +
                        "FROM Servicii S " +
                        "JOIN Evenimente_Servicii ES ON ES.IDServiciu = S.IDServiciu " +
                        "JOIN Evenimente E ON E.IDEveniment = ES.IDEveniment " +
                        "WHERE YEAR(E.Data_Inceput) = ? " +
                        "GROUP BY S.IDServiciu, S.Denumire " +
                        "HAVING SUM(S.Pret * ES.Cantitate) > ( " +
                        "   SELECT AVG(rev) FROM ( " +
                        "       SELECT SUM(S2.Pret * ES2.Cantitate) AS rev " +
                        "       FROM Servicii S2 " +
                        "       JOIN Evenimente_Servicii ES2 ON ES2.IDServiciu = S2.IDServiciu " +
                        "       JOIN Evenimente E2 ON E2.IDEveniment = ES2.IDEveniment " +
                        "       WHERE YEAR(E2.Data_Inceput) = ? " +
                        "       GROUP BY S2.IDServiciu " +
                        "   ) t " +
                        ") " +
                        "ORDER BY SUM(S.Pret * ES.Cantitate) DESC";

        populateTable(sql, year, year);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) dynamicTable.getScene().getWindow();
        stage.close();
    }
}
