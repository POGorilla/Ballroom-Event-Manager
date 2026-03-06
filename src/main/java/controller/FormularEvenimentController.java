package controller;

import database.DatabaseManager;
import model.AngajatModel;
import model.ClientModel;
import model.SalaModel;
import model.ServiciuModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Optional;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * Controller Formular Eveniment
 * Fereastra pt Adaugare/Editare eveniment + alocare resurse (angajati/servicii).
 */
public class FormularEvenimentController {
    @FXML private Label titleLabel;
    @FXML private TextField denumireField;
    @FXML private ComboBox<ClientModel> clientComboBox;
    @FXML private ComboBox<SalaModel> salaComboBox;
    @FXML private DatePicker dataInceputPicker;
    @FXML private DatePicker dataSfarsitPicker;
    @FXML private TextField nrPersoaneField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField pretField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    @FXML private ListView<AngajatModel> availableAngajatiList;
    @FXML private ListView<AngajatModel> assignedAngajatiList;
    @FXML private Button addAngajatBtn;
    @FXML private Button removeAngajatBtn;

    @FXML private ListView<ServiciuModel> availableServiciiList;
    @FXML private ListView<ServiciuAlocatModel> assignedServiciiList;
    @FXML private Button addServiciuBtn;
    @FXML private Button removeServiciuBtn;

    private ObservableList<ServiciuModel> availableServicii = FXCollections.observableArrayList();
    private ObservableList<ServiciuAlocatModel> assignedServicii = FXCollections.observableArrayList();
    private ObservableList<ClientModel> listaClienti = FXCollections.observableArrayList();
    private ObservableList<SalaModel> listaSali = FXCollections.observableArrayList();
    private ObservableList<AngajatModel> availableAngajati = FXCollections.observableArrayList();
    private ObservableList<AngajatModel> assignedAngajati = FXCollections.observableArrayList();

    private Integer currentEventId = null; // null ==> Add, ID ==> Edit

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList("Planificat", "Finalizat", "Anulat"));

        loadClienti();
        loadSali();
        loadAllAngajati();
        loadAllServicii();

        clientComboBox.setItems(listaClienti);
        salaComboBox.setItems(listaSali);

        availableAngajatiList.setItems(availableAngajati);
        assignedAngajatiList.setItems(assignedAngajati);

        availableServiciiList.setItems(availableServicii);
        assignedServiciiList.setItems(assignedServicii);

        // Cand schimb sala ==> Recalculez pretul total
        salaComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            recalculateTotalCost();
        });
    }

    // Setare mod (Add / Edit)
    public void initData(Integer eventId) {
        this.currentEventId = eventId;
        if (eventId == null) {
            titleLabel.setText("Adauga Eveniment Nou");
            statusComboBox.setValue("Planificat");
        } else {
            titleLabel.setText("Modifica Eveniment");
            loadEventData(eventId);
            loadAssignedAngajati(eventId);
            loadAssignedServicii(eventId);
        }
    }

    // Buton Save ==> Validare + Operatii DB
    @FXML
    private void handleSave() {
        // validare: campuri obligatorii
        if (denumireField.getText().trim().isEmpty() ||
                clientComboBox.getValue() == null ||
                salaComboBox.getValue() == null ||
                statusComboBox.getValue() == null) {

            showError("Toate campurile principale (Denumire, Client, Sala, Status) sunt obligatorii!");
            return;
        }

        // Validare date calendaristice
        LocalDate start = dataInceputPicker.getValue();
        LocalDate end = dataSfarsitPicker.getValue();

        if (start == null || end == null) {
            showError("Selectati datele de inceput si sfarsit!");
            return;
        }

        if (end.isBefore(start)) {
            showError("Data de sfarsit nu poate fi inaintea datei de inceput!");
            return;
        }

        // Validare nr persoane
        try {
            int pers = Integer.parseInt(nrPersoaneField.getText().trim());
            if (pers <= 0) {
                showError("Numarul de persoane trebuie sa fie mai mare decat 0.");
                return;
            }

            int cap = getCapacitateSala(salaComboBox.getValue().getId());
            if (cap > 0 && pers > cap) {
                showError("Numarul de persoane (" + pers + ") depaseste capacitatea salii selectate (" + cap + ").");
                return;
            }

        } catch (NumberFormatException e) {
            showError("Numarul de persoane invalid! Introduceti un numar intreg.");
            return;
        }

        // Validare pret
        try {
            double price = Double.parseDouble(pretField.getText().replace(",", ".").trim());
            if (price < 0) {
                showError("Pretul total nu poate fi negativ.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Pretul total este invalid! Verificati formatul.");
            return;
        }

        // queryuri db
        try {
            int eventId;
            if (currentEventId == null) {
                eventId = runInsert(); // Adaugare
            } else {
                runUpdate(); // Modificare
                eventId = currentEventId;
            }

            // Salvare relatii N:N (Angajati + Servicii)
            saveAngajati(eventId);
            saveServicii(eventId);

            showInfo("Eveniment salvat cu succes!");
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Eroare SQL: " + e.getMessage());
        }
    }

    // SQL: INSERT ==> Adauga evenimentul in tabela principala
    private int runInsert() throws SQLException {
        String sql = "INSERT INTO Evenimente (Denumire, Data_Inceput, Data_Sfarsit, Nr_Persoane, Status, IDSala, IDClient, Pret_Total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = -1;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, denumireField.getText().trim());
            pstmt.setDate(2, Date.valueOf(dataInceputPicker.getValue()));
            pstmt.setDate(3, Date.valueOf(dataSfarsitPicker.getValue()));
            pstmt.setInt(4, Integer.parseInt(nrPersoaneField.getText().trim()));
            pstmt.setString(5, statusComboBox.getValue());
            pstmt.setInt(6, salaComboBox.getValue().getId());
            pstmt.setInt(7, clientComboBox.getValue().getId());
            pstmt.setDouble(8, Double.parseDouble(pretField.getText().replace(",", ".").trim()));

            pstmt.executeUpdate();

            // Returnam ID-ul generat pt a lega angajatii/serviciile
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) generatedId = rs.getInt(1);
            }
        }
        return generatedId;
    }

    // SQL: UPDATE ==> Modifica eveniment existent
    private void runUpdate() throws SQLException {
        String sql = "UPDATE Evenimente SET Denumire = ?, Data_Inceput = ?, Data_Sfarsit = ?, Nr_Persoane = ?, " +
                "Status = ?, IDSala = ?, IDClient = ?, Pret_Total = ? WHERE IDEveniment = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, denumireField.getText().trim());
            pstmt.setDate(2, Date.valueOf(dataInceputPicker.getValue()));
            pstmt.setDate(3, Date.valueOf(dataSfarsitPicker.getValue()));
            pstmt.setInt(4, Integer.parseInt(nrPersoaneField.getText().trim()));
            pstmt.setString(5, statusComboBox.getValue());
            pstmt.setInt(6, salaComboBox.getValue().getId());
            pstmt.setInt(7, clientComboBox.getValue().getId());
            pstmt.setDouble(8, Double.parseDouble(pretField.getText().replace(",", ".").trim()));
            pstmt.setInt(9, currentEventId);

            pstmt.executeUpdate();
        }
    }

    // Tranzactie SQL: Sterge vechile legaturi Angajati ==> Insereaza noile legaturi
    private void saveAngajati(int eventId) throws SQLException {
        String deleteSql = "DELETE FROM Evenimente_Angajati WHERE IDEveniment = ?";
        String insertSql = "INSERT INTO Evenimente_Angajati (IDEveniment, IDAngajat, Rol) VALUES (?, ?, ?)";

        Connection conn = DatabaseManager.getConnection();
        try {
            conn.setAutoCommit(false); // Start Tranzactie
            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                deletePstmt.setInt(1, eventId);
                deletePstmt.executeUpdate();
            }
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                for (AngajatModel angajat : assignedAngajati) {
                    insertPstmt.setInt(1, eventId);
                    insertPstmt.setInt(2, angajat.getId());
                    insertPstmt.setString(3, angajat.getFunctie());
                    insertPstmt.addBatch(); // Batch insert pt eficienta
                }
                insertPstmt.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // Tranzactie SQL: Sterge vechile legaturi Servicii ==> Insereaza noile legaturi
    private void saveServicii(int eventId) throws SQLException {
        String deleteSql = "DELETE FROM Evenimente_Servicii WHERE IDEveniment = ?";
        String insertSql = "INSERT INTO Evenimente_Servicii (IDEveniment, IDServiciu, Cantitate) VALUES (?, ?, ?)";

        Connection conn = DatabaseManager.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                deletePstmt.setInt(1, eventId);
                deletePstmt.executeUpdate();
            }
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                for (ServiciuAlocatModel serviciu : assignedServicii) {
                    insertPstmt.setInt(1, eventId);
                    insertPstmt.setInt(2, serviciu.getId());
                    insertPstmt.setInt(3, serviciu.getCantitate());
                    insertPstmt.addBatch();
                }
                insertPstmt.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // query 11 SQL: SELECT ==> Incarcare date eveniment (pt Edit)
    private void loadEventData(int eventId) {
        String sql = "SELECT * FROM Evenimente WHERE IDEveniment = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                denumireField.setText(rs.getString("Denumire"));
                dataInceputPicker.setValue(rs.getDate("Data_Inceput").toLocalDate());
                dataSfarsitPicker.setValue(rs.getDate("Data_Sfarsit").toLocalDate());
                nrPersoaneField.setText(String.valueOf(rs.getInt("Nr_Persoane")));
                statusComboBox.setValue(rs.getString("Status"));
                pretField.setText(String.valueOf(rs.getDouble("Pret_Total")));

                int idClient = rs.getInt("IDClient");
                for (ClientModel c : listaClienti) if (c.getId() == idClient) clientComboBox.setValue(c);

                int idSala = rs.getInt("IDSala");
                for (SalaModel s : listaSali) if (s.getId() == idSala) salaComboBox.setValue(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // query 12 SQL: SELECT ==> Incarcare liste pt dropdowns
    private void loadClienti() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT IDClient, Nume, Prenume FROM Clienti ORDER BY Nume, Prenume")) {
            while (rs.next()) listaClienti.add(new ClientModel(rs.getInt("IDClient"), rs.getString("Nume") + " " + rs.getString("Prenume")));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadSali() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             // query 13
             ResultSet rs = stmt.executeQuery("SELECT IDSala, Denumire, Pret_ora FROM Sali WHERE Disponibilitate = 1 ORDER BY Denumire")) {
            while (rs.next()) listaSali.add(new SalaModel(rs.getInt("IDSala"), rs.getString("Denumire"), rs.getDouble("Pret_ora")));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAllAngajati() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             // query 14
             ResultSet rs = stmt.executeQuery("SELECT IDAngajat, Nume, Prenume, Functie FROM Angajati ORDER BY Nume, Prenume")) {
            while (rs.next()) availableAngajati.add(new AngajatModel(rs.getInt("IDAngajat"), rs.getString("Nume"), rs.getString("Prenume"), rs.getString("Functie")));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAllServicii() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             // query 15
             ResultSet rs = stmt.executeQuery("SELECT IDServiciu, Denumire, Pret FROM Servicii ORDER BY Denumire")) {
            while (rs.next()) availableServicii.add(new ServiciuModel(rs.getInt("IDServiciu"), rs.getString("Denumire"), rs.getDouble("Pret")));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Logica incarcare angajati deja alocati (pt Edit)
    private void loadAssignedAngajati(int eventId) {
        List<Integer> assignedIds = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             // query 16
             PreparedStatement pstmt = conn.prepareStatement("SELECT IDAngajat FROM Evenimente_Angajati WHERE IDEveniment = ?")) {
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) assignedIds.add(rs.getInt("IDAngajat"));
        } catch (SQLException e) { e.printStackTrace(); }

        List<AngajatModel> toMove = new ArrayList<>();
        for (AngajatModel a : availableAngajati) if (assignedIds.contains(a.getId())) toMove.add(a);
        assignedAngajati.addAll(toMove);
        availableAngajati.removeAll(toMove);
    }

    // Logica incarcare servicii deja alocate (pt Edit) + cantitatea asociata
    private void loadAssignedServicii(int eventId) {
        // query 17
        String sql = "SELECT S.IDServiciu, S.Denumire, S.Pret, ES.Cantitate " +
                "FROM Servicii S " +
                "JOIN Evenimente_Servicii ES ON S.IDServiciu = ES.IDServiciu " +
                "WHERE ES.IDEveniment = ? " +
                "ORDER BY S.Denumire";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("IDServiciu");
                String denumire = rs.getString("Denumire");
                double pret = rs.getDouble("Pret");
                int cant = rs.getInt("Cantitate");
                if (cant <= 0) cant = 1;

                assignedServicii.add(new ServiciuAlocatModel(id, denumire, pret, cant));

                // scoatem serviciul din lista de disponibile (ca sa nu poata fi alocat de 2 ori)
                for (int i = availableServicii.size() - 1; i >= 0; i--) {
                    if (availableServicii.get(i).getId() == id) {
                        availableServicii.remove(i);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        recalculateTotalCost();
    }

    // Buton [>] Angajat
    @FXML private void handleAddAngajat() {
        AngajatModel sel = availableAngajatiList.getSelectionModel().getSelectedItem();
        if (sel != null) { availableAngajati.remove(sel); assignedAngajati.add(sel); }
    }

    // Buton [<] Angajat
    @FXML private void handleRemoveAngajat() {
        AngajatModel sel = assignedAngajatiList.getSelectionModel().getSelectedItem();
        if (sel != null) { assignedAngajati.remove(sel); availableAngajati.add(sel); }
    }

    // Buton [>] Serviciu (cere Cantitate la fiecare adaugare)
    @FXML private void handleAddServiciu() {
        ServiciuModel sel = availableServiciiList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Integer cantitate = promptCantitate(sel);
        if (cantitate == null) return; // Cancel

        availableServicii.remove(sel);
        assignedServicii.add(new ServiciuAlocatModel(sel.getId(), sel.getDenumire(), sel.getPret(), cantitate));
        recalculateTotalCost();
    }

    // Buton [<] Serviciu
    @FXML private void handleRemoveServiciu() {
        ServiciuAlocatModel sel = assignedServiciiList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        assignedServicii.remove(sel);
        availableServicii.add(new ServiciuModel(sel.getId(), sel.getDenumire(), sel.getPret()));
        FXCollections.sort(availableServicii, (a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        recalculateTotalCost();
    }

    // Dialog: cantitatea pentru un serviciu (validari: obligatoriu, int, pozitiv)
    private Integer promptCantitate(ServiciuModel serviciu) {
        while (true) {
            TextInputDialog dlg = new TextInputDialog("1");
            dlg.setTitle("Cantitate serviciu");
            dlg.setHeaderText("Introduceti cantitatea pentru serviciul: " + serviciu.getDenumire());
            dlg.setContentText("Cantitate (numar intreg, minim 1):");

            Optional<String> result = dlg.showAndWait();
            if (result.isEmpty()) return null; // Cancel

            String raw = result.get().trim();
            if (raw.isEmpty()) {
                showError("Cantitatea este obligatorie!");
                continue;
            }

            try {
                int q = Integer.parseInt(raw);
                if (q <= 0) {
                    showError("Cantitatea trebuie sa fie >= 1.");
                    continue;
                }
                if (q > 100000) {
                    showError("Cantitatea este prea mare (maxim 100000).");
                    continue;
                }
                return q;
            } catch (NumberFormatException e) {
                showError("Cantitatea trebuie sa fie un numar intreg valid.");
            }
        }
    }

    // Calcul automat: Pret Sala (8h) + Suma servicii (Pret x Cantitate)
    private void recalculateTotalCost() {
        double total = 0.0;
        SalaModel selectedSala = salaComboBox.getValue();
        if (selectedSala != null) total += selectedSala.getPretOra() * 8;

        for (ServiciuAlocatModel s : assignedServicii) {
            total += s.getPret() * s.getCantitate();
        }

        pretField.setText(String.format("%.2f", total));
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare Validare");
        alert.setHeaderText("Date invalide");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private int getCapacitateSala(int idSala) {
        // query 44
        String sql = "SELECT Capacitate FROM Sali WHERE IDSala = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Capacitate");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Model local pentru servicii alocate (cu cantitate)
    public static class ServiciuAlocatModel {
        private final int id;
        private final String denumire;
        private final double pret;
        private final int cantitate;

        public ServiciuAlocatModel(int id, String denumire, double pret, int cantitate) {
            this.id = id;
            this.denumire = denumire;
            this.pret = pret;
            this.cantitate = cantitate;
        }

        public int getId() { return id; }
        public String getDenumire() { return denumire; }
        public double getPret() { return pret; }
        public int getCantitate() { return cantitate; }

        @Override
        public String toString() {
            return denumire + " (" + String.format("%.2f", pret) + " RON x " + cantitate + " buc)";
        }
    }
}