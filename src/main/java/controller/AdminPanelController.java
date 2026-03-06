package controller;

import database.DatabaseManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.mindrot.jbcrypt.BCrypt;
import utils.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/*
 * Controller Admin Panel
 * Gestioneaza:
 * 1. Angajati
 * 2. Utilizatori (ai aplicatiei)
 * 3. Login Logs
 */
public class AdminPanelController {

    // elemente ui
    @FXML private TableView<AngajatModel> angajatiTable;
    @FXML private TableColumn<AngajatModel, String> colAngNume, colAngPrenume, colAngFunctie, colAngTelefon, colAngEmail, colAngSupervisorName;
    @FXML private TableColumn<AngajatModel, Double> colAngSalariu;

    @FXML private TextField tfAngNume, tfAngPrenume, tfAngTelefon, tfAngEmail, tfAngSalariu;
    @FXML private ComboBox<String> cbAngFunctie;
    @FXML private ComboBox<AngajatModel> cbAngSupervisor;

    @FXML private TableView<UserModel> usersTable;
    @FXML private TableColumn<UserModel, String> colUserUser, colUserNume, colUserPrenume, colUserRol, colUserAngajatNume;

    @FXML private TextField tfUserUser, tfUserNume, tfUserPrenume;
    @FXML private PasswordField tfUserPass;
    @FXML private ComboBox<String> cbUserRol;
    @FXML private ComboBox<AngajatModel> cbUserLinkAngajat;
    @FXML private CheckBox chkUserExtern;

    @FXML private TableView<LogModel> logsTable;
    @FXML private TableColumn<LogModel, String> colLogUserTried, colLogDate, colLogStatus;

    // --- SALI ---
    @FXML private TableView<SalaAdminModel> saliTable;
    @FXML private TableColumn<SalaAdminModel, String> colSalaDenumire;
    @FXML private TableColumn<SalaAdminModel, Integer> colSalaCapacitate;
    @FXML private TableColumn<SalaAdminModel, Double> colSalaPretOra;
    @FXML private TableColumn<SalaAdminModel, String> colSalaDisponibil;

    @FXML private TextField tfSalaDenumire, tfSalaCapacitate, tfSalaPretOra;
    @FXML private CheckBox chkSalaDisponibila;

    // --- SERVICII ---
    @FXML private TableView<ServiciuAdminModel> serviciiTable;
    @FXML private TableColumn<ServiciuAdminModel, String> colServDenumire;
    @FXML private TableColumn<ServiciuAdminModel, Double> colServPret;
    @FXML private TableColumn<ServiciuAdminModel, String> colServDescriere;

    @FXML private TextField tfServDenumire, tfServPret;
    @FXML private TextArea taServDescriere;

    // init ==> Se executa la deschiderea ferestrei
    @FXML
    public void initialize() {
        setupAngajatiTable();
        setupUsersTable();
        setupLogsTable();
        setupSaliTable();
        setupServiciiTable();

        // Populam dropdown-urile cu valori statice
        loadFunctiiCombo();
        cbUserRol.setItems(FXCollections.observableArrayList("admin", "manager", "supervizor", "angajat", "tester app"));

        setupUserCreationLogic();
    }

    // incarca lista de functii ale angajatilor
    private void loadFunctiiCombo() {
        ObservableList<String> items = FXCollections.observableArrayList(
                "Administrator",
                "Director General",
                "Manager General",
                "Manager Evenimente",
                "Coordonator Evenimente",
                "Organizator Evenimente",
                "Event Planner",
                "Manager Sala",
                "Supervisor Sala",
                "Receptie",
                "Receptioner",
                "Operator Rezervari",
                "Secretara",

                "Manager Vanzari",
                "Agent Vanzari",
                "Reprezentant Vanzari",
                "Manager Marketing",
                "Specialist Marketing",
                "PR / Comunicare",

                "Contabil",
                "Contabil Sef",
                "Casier",
                "Resurse Umane",
                "Achizitii",
                "Magazioner",

                "Sef Ospatar",
                "Ospatar",
                "Ajutor Ospatar",
                "Host / Hostess",
                "Barman",
                "Barista",
                "Somelier",

                "Bucatar Sef",
                "Bucatar",
                "Ajutor Bucatar",
                "Cofetar",
                "Patiser",
                "Spalator vase",

                "Tehnician Sunet",
                "Tehnician Lumini",
                "Tehnician IT",
                "Electrician",
                "Instalator",
                "Intretinere",
                "Manipulant Marfa",

                "Curatenie",
                "Menajera",
                "Supraveghetor Curatenie",

                "Securitate",
                "Agent Securitate",
                "Paznic",

                "Sofer",
                "Valet",

                "DJ",
                "MC / Prezentator",
                "Decorator",
                "Florist",
                "Fotograf",
                "Videograf"
        );

        // 2) Adauga automat orice functie care exista deja in DB (si nu e în lista)
        // query 43
        String sql = "SELECT DISTINCT Functie FROM Angajati " +
                "WHERE Functie IS NOT NULL AND TRIM(Functie) <> ''";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String f = rs.getString(1);
                if (f == null) continue;
                f = f.trim();
                if (!f.isEmpty() && !items.contains(f)) {
                    items.add(f);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // sortare alfabetica
        FXCollections.sort(items);

        cbAngFunctie.setItems(items);
    }

    // -------------------------
    // SALI
    // -------------------------
    private void setupSaliTable() {
        if (saliTable == null) return;
        colSalaDenumire.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().denumire));
        colSalaCapacitate.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().capacitate).asObject());
        colSalaPretOra.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().pretOra).asObject());
        colSalaDisponibil.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().disponibil ? "DA" : "NU"));

        // Click pe rand ==> populare campuri (pentru update)
        saliTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            tfSalaDenumire.setText(newVal.denumire);
            tfSalaCapacitate.setText(String.valueOf(newVal.capacitate));
            tfSalaPretOra.setText(String.format("%.2f", newVal.pretOra));
            chkSalaDisponibila.setSelected(newVal.disponibil);
        });

        // Default
        if (chkSalaDisponibila != null) chkSalaDisponibila.setSelected(true);

        refreshSali();
    }

    private void refreshSali() {
        if (saliTable == null) return;
        ObservableList<SalaAdminModel> list = FXCollections.observableArrayList();

        // query 32: citeste toate salile din tabelul Sali, ordonate alfabetic dupa denumire
        String sql = "SELECT IDSala, Denumire, Capacitate, Pret_ora, Disponibilitate FROM Sali ORDER BY Denumire";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new SalaAdminModel(
                        rs.getInt("IDSala"),
                        rs.getString("Denumire"),
                        rs.getInt("Capacitate"),
                        rs.getDouble("Pret_ora"),
                        rs.getBoolean("Disponibilitate")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Eroare la incarcarea salilor: " + e.getMessage());
        }

        saliTable.setItems(list);
    }

    @FXML
    private void handleAddSala() {
        SalaInput salaInput = readAndValidateSalaInput(false);
        if (salaInput == null) return;

        if (!showConfirmation("Sunteti sigur ca doriti sa adaugati sala '" + salaInput.denumire + "'?")) return;

        // query 33
        String sql = "INSERT INTO Sali (Denumire, Capacitate, Pret_ora, Disponibilitate) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, salaInput.denumire);
            pstmt.setInt(2, salaInput.capacitate);
            pstmt.setDouble(3, salaInput.pretOra);
            pstmt.setBoolean(4, salaInput.disponibil);
            pstmt.executeUpdate();

            refreshSali();
            handleClearSala();
            showInfo("Sala a fost adaugata cu succes!");
        } catch (SQLException e) {
            // Duplicate denumire, etc.
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                showError("Exista deja o sala cu aceasta denumire!");
            } else {
                showError("Eroare SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdateSala() {
        SalaAdminModel sel = (saliTable != null) ? saliTable.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            showError("Selectati o sala din tabel pentru modificare.");
            return;
        }

        SalaInput salaInput = readAndValidateSalaInput(true);
        if (salaInput == null) return;

        if (!showConfirmation("Sunteti sigur ca doriti sa modificati sala ?")) return;

        // query 34
        String sql = "UPDATE Sali SET Denumire = ?, Capacitate = ?, Pret_ora = ?, Disponibilitate = ? WHERE IDSala = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, salaInput.denumire);
            pstmt.setInt(2, salaInput.capacitate);
            pstmt.setDouble(3, salaInput.pretOra);
            pstmt.setBoolean(4, salaInput.disponibil);
            pstmt.setInt(5, sel.id);

            pstmt.executeUpdate();
            refreshSali();
            showInfo("Sala a fost modificata cu succes!");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                showError("Exista deja o sala cu aceasta denumire!");
            } else {
                showError("Eroare SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteSala() {
        SalaAdminModel sel = (saliTable != null) ? saliTable.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            showError("Selectati o sala pentru stergere.");
            return;
        }

        // Check relatie FK: Evenimente -> Sali
        // query 35
        int usedCount = countRows("SELECT COUNT(*) FROM Evenimente WHERE IDSala = ?", sel.id);
        if (usedCount > 0) {
            showError("Nu se poate sterge aceasta sala deoarece este folosita in " + usedCount + " eveniment(e).\n" +
                    "Recomandare: setati Disponibilitate = NU.");
            return;
        }

        if (!showConfirmation("Sunteti sigur ca doriti sa stergeti sala '" + sel.denumire + " ?")) return;

        try (Connection conn = DatabaseManager.getConnection();
             // query 36
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Sali WHERE IDSala = ?")) {
            pstmt.setInt(1, sel.id);
            pstmt.executeUpdate();
            refreshSali();
            handleClearSala();
            showInfo("Sala a fost stearsa cu succes!");
        } catch (SQLException e) {
            showError("Eroare la stergere: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearSala() {
        if (tfSalaDenumire != null) tfSalaDenumire.clear();
        if (tfSalaCapacitate != null) tfSalaCapacitate.clear();
        if (tfSalaPretOra != null) tfSalaPretOra.clear();
        if (chkSalaDisponibila != null) chkSalaDisponibila.setSelected(true);
        if (saliTable != null) saliTable.getSelectionModel().clearSelection();
    }

    private SalaInput readAndValidateSalaInput(boolean isUpdate) {
        String denumire = tfSalaDenumire.getText() == null ? "" : tfSalaDenumire.getText().trim();
        String capacitateStr = tfSalaCapacitate.getText() == null ? "" : tfSalaCapacitate.getText().trim();
        String pretOraStr = tfSalaPretOra.getText() == null ? "" : tfSalaPretOra.getText().trim().replace(",", ".");
        boolean disponibil = chkSalaDisponibila != null && chkSalaDisponibila.isSelected();

        if (denumire.isEmpty() || capacitateStr.isEmpty() || pretOraStr.isEmpty()) {
            showError("Denumire, Capacitate si Pret/Ora sunt obligatorii!");
            return null;
        }

        if (denumire.length() < 3) {
            showError("Denumirea salii este prea scurta (minim 3 caractere).");
            return null;
        }

        if (denumire.length() > 50) {
            showError("Denumirea salii este prea lunga (maxim 50 caractere).");
            return null;
        }

        int capacitate;
        try {
            capacitate = Integer.parseInt(capacitateStr);
            if (capacitate <= 0) {
                showError("Capacitatea trebuie sa fie mai mare decat 0.");
                return null;
            }
            if (capacitate > 10000) {
                showError("Capacitatea este nerealista (maxim 10000).");
                return null;
            }
        } catch (NumberFormatException e) {
            showError("Capacitatea trebuie sa fie un numar intreg.");
            return null;
        }

        double pretOra;
        try {
            pretOra = Double.parseDouble(pretOraStr);
            if (pretOra < 0) {
                showError("Pretul pe ora nu poate fi negativ.");
                return null;
            }
        } catch (NumberFormatException e) {
            showError("Pretul pe ora trebuie sa fie un numar valid (ex: 250 sau 250.50).");
            return null;
        }

        return new SalaInput(denumire, capacitate, pretOra, disponibil);
    }

    // -------------------------
    // SERVICII CRUD
    // -------------------------
    private void setupServiciiTable() {
        if (serviciiTable == null) return; // FXML safety
        colServDenumire.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().denumire));
        colServPret.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().pret).asObject());
        colServDescriere.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().descriereScurta));

        serviciiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            tfServDenumire.setText(newVal.denumire);
            tfServPret.setText(String.format("%.2f", newVal.pret));
            taServDescriere.setText(newVal.descriere == null ? "" : newVal.descriere);
        });

        refreshServicii();
    }

    private void refreshServicii() {
        if (serviciiTable == null) return;
        ObservableList<ServiciuAdminModel> list = FXCollections.observableArrayList();

        //query 37
        String sql = "SELECT IDServiciu, Denumire, Descriere, Pret FROM Servicii ORDER BY Denumire";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ServiciuAdminModel(
                        rs.getInt("IDServiciu"),
                        rs.getString("Denumire"),
                        rs.getString("Descriere"),
                        rs.getDouble("Pret")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Eroare la incarcarea serviciilor: " + e.getMessage());
        }

        serviciiTable.setItems(list);
    }

    @FXML
    private void handleAddServiciu() {
        ServiciuInput input = readAndValidateServiciuInput();
        if (input == null) return;

        if (!showConfirmation("Sunteti sigur ca doriti sa adaugati serviciul '" + input.denumire + "'?")) return;

        // query 38
        String sql = "INSERT INTO Servicii (Denumire, Descriere, Pret) VALUES (?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, input.denumire);
            if (input.descriere == null || input.descriere.isBlank()) pstmt.setObject(2, null);
            else pstmt.setString(2, input.descriere);
            pstmt.setDouble(3, input.pret);
            pstmt.executeUpdate();

            refreshServicii();
            handleClearServiciu();
            showInfo("Serviciul a fost adaugat cu succes!");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                showError("Exista deja un serviciu cu aceasta denumire!");
            } else {
                showError("Eroare SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdateServiciu() {
        ServiciuAdminModel sel = (serviciiTable != null) ? serviciiTable.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            showError("Selectati un serviciu din tabel pentru modificare.");
            return;
        }

        ServiciuInput input = readAndValidateServiciuInput();
        if (input == null) return;

        if (!showConfirmation("Sunteti sigur ca doriti sa modificati serviciul?")) return;

        // query 39
        String sql = "UPDATE Servicii SET Denumire = ?, Descriere = ?, Pret = ? WHERE IDServiciu = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, input.denumire);
            if (input.descriere == null || input.descriere.isBlank()) pstmt.setObject(2, null);
            else pstmt.setString(2, input.descriere);
            pstmt.setDouble(3, input.pret);
            pstmt.setInt(4, sel.id);

            pstmt.executeUpdate();
            refreshServicii();
            showInfo("Serviciul a fost modificat cu succes!");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                showError("Exista deja un serviciu cu aceasta denumire!");
            } else {
                showError("Eroare SQL: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteServiciu() {
        ServiciuAdminModel sel = (serviciiTable != null) ? serviciiTable.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            showError("Selectati un serviciu pentru stergere.");
            return;
        }

        // Check relatie FK: Evenimente_Servicii -> Servicii
        // query 40
        int usedCount = countRows("SELECT COUNT(*) FROM Evenimente_Servicii WHERE IDServiciu = ?", sel.id);
        if (usedCount > 0) {
            showError("Nu se poate sterge acest serviciu deoarece este folosit in " + usedCount + " eveniment(e).\n" +
                    "Stergeti mai intai legaturile din evenimente sau pastrati serviciul activ.");
            return;
        }

        if (!showConfirmation("Sunteti sigur ca doriti sa stergeti serviciul " + sel.denumire)) return;

        try (Connection conn = DatabaseManager.getConnection();
             // query 41
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Servicii WHERE IDServiciu = ?")) {
            pstmt.setInt(1, sel.id);
            pstmt.executeUpdate();
            refreshServicii();
            handleClearServiciu();
            showInfo("Serviciul a fost sters cu succes!");
        } catch (SQLException e) {
            showError("Eroare la stergere: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearServiciu() {
        if (tfServDenumire != null) tfServDenumire.clear();
        if (tfServPret != null) tfServPret.clear();
        if (taServDescriere != null) taServDescriere.clear();
        if (serviciiTable != null) serviciiTable.getSelectionModel().clearSelection();
    }

    private ServiciuInput readAndValidateServiciuInput() {
        String denumire = tfServDenumire.getText() == null ? "" : tfServDenumire.getText().trim();
        String pretStr = tfServPret.getText() == null ? "" : tfServPret.getText().trim().replace(",", ".");
        String descriere = taServDescriere.getText();
        descriere = (descriere == null) ? "" : descriere.trim();

        if (denumire.isEmpty() || pretStr.isEmpty()) {
            showError("Denumire si Pret sunt obligatorii!");
            return null;
        }

        if (denumire.length() < 3) {
            showError("Denumirea serviciului este prea scurta (minim 3 caractere).");
            return null;
        }
        if (denumire.length() > 50) {
            showError("Denumirea serviciului este prea lunga (maxim 50 caractere).");
            return null;
        }

        if (!descriere.isEmpty()) {
            if (descriere.length() < 5) {
                showError("Descrierea este prea scurta (minim 5 caractere) sau lasati campul gol.");
                return null;
            }
            if (descriere.length() > 1000) {
                showError("Descrierea este prea lunga (maxim 1000 caractere).");
                return null;
            }
        }

        double pret;
        try {
            pret = Double.parseDouble(pretStr);
            if (pret < 0) {
                showError("Pretul nu poate fi negativ.");
                return null;
            }
        } catch (NumberFormatException e) {
            showError("Pretul trebuie sa fie un numar valid (ex: 100 sau 100.50).");
            return null;
        }

        return new ServiciuInput(denumire, descriere.isEmpty() ? null : descriere, pret);
    }

    private int countRows(String sql, int idParam) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idParam);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Configurare tabel Angajati (legare coloane <-> model)
    private void setupAngajatiTable() {
        colAngNume.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nume));
        colAngPrenume.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().prenume));
        colAngFunctie.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().functie));
        colAngTelefon.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().telefon));
        colAngEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email));
        colAngSalariu.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().salariu).asObject());
        colAngSupervisorName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().numeSupervizor));

        setupAngajatComboBox(cbAngSupervisor);
        refreshAngajati();
    }

    // Incarcare date Angajati din DB
    private void refreshAngajati() {
        ObservableList<AngajatModel> list = FXCollections.observableArrayList();

        // query 1 ////// SQL: SELECT + SELF JOIN ==> iau angajatii si numele sefului (din ID)
        String sql = "SELECT A.*, S.Nume AS SupNume, S.Prenume AS SupPrenume " +
                "FROM Angajati A " +
                "LEFT JOIN Angajati S ON A.IDSupervizor = S.IDAngajat";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Formatare string sef: "Nume Prenume (ID)"
                String supName = "-";
                if(rs.getInt("IDSupervizor") != 0) {
                    supName = rs.getString("SupNume") + " " + rs.getString("SupPrenume");
                }

                list.add(new AngajatModel(
                        rs.getInt("IDAngajat"), rs.getString("Nume"), rs.getString("Prenume"),
                        rs.getString("Functie"), rs.getString("Telefon"), rs.getString("Email"),
                        rs.getDouble("Salariu"), rs.getInt("IDSupervizor"), supName
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }

        angajatiTable.setItems(list);
        cbAngSupervisor.setItems(list);
        cbUserLinkAngajat.setItems(list);
    }

    // Buton Adauga Angajat ==> Validare + Insert DB
    @FXML
    private void handleAddAngajat() {
        String nume = tfAngNume.getText().trim();
        String prenume = tfAngPrenume.getText().trim();
        String functie = cbAngFunctie.getValue();
        String telefon = tfAngTelefon.getText().trim();
        String email = tfAngEmail.getText().trim();
        String salariuStr = tfAngSalariu.getText().trim();

        // Validare: campuri goale
        if (nume.isEmpty() || prenume.isEmpty() || functie == null || telefon.isEmpty() || email.isEmpty() || salariuStr.isEmpty()) {
            showError("Toate campurile sunt obligatorii!");
            return;
        }

        // Validare: format telefon (07xxxxxxxx)
        if (!telefon.matches("^07\\d{8}$")) {
            showError("Numar de telefon invalid! Trebuie sa inceapa cu '07' si sa aiba 10 cifre.");
            return;
        }

        // Validare: format email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format Email invalid! Exemplu: angajat@ballroom.ro");
            return;
        }

        double salariu;
        try {
            salariu = Double.parseDouble(salariuStr);
            if (salariu <= 0) {
                showError("Salariul trebuie sa fie mai mare decat 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Salariul trebuie sa fie un numar valid (ex: 3500.50).");
            return;
        }

        // Confirmare user
        if (!showConfirmation("Sunteti sigur ca doriti sa adaugati acest angajat?")) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             // SQL: INSERT ==> Adauga angajat nou
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO Angajati (Nume, Prenume, Functie, Telefon, Email, Salariu, IDSupervizor) VALUES (?,?,?,?,?,?,?)")) {

            pstmt.setString(1, nume);
            pstmt.setString(2, prenume);
            pstmt.setString(3, functie);
            pstmt.setString(4, telefon);
            pstmt.setString(5, email);
            pstmt.setDouble(6, salariu);

            // Setare sef (daca e selectat)
            AngajatModel supervisor = cbAngSupervisor.getValue();
            if(supervisor == null) pstmt.setObject(7, null);
            else pstmt.setInt(7, supervisor.id);

            pstmt.executeUpdate();
            refreshAngajati();
            clearAngajatiFields();
            showInfo("Angajat adaugat cu succes!");

        } catch (Exception e) { showError("Eroare la baza de date: " + e.getMessage()); }
    }

    // Buton Sterge Angajat ==> Tranzactie (Update Subalterni + Delete)
    @FXML
    private void handleDeleteAngajat() {
        AngajatModel sel = angajatiTable.getSelectionModel().getSelectedItem();
        if(sel == null) {
            showError("Selectati un angajat pentru a sterge.");
            return;
        }

        if (!showConfirmation("Sunteti sigur ca doriti sa stergeti angajatul " + sel.nume + " " + sel.prenume + "?")) {
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // SQL UPDATE: Subalternii raman fara sef (IDSupervizor = NULL)
            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE Angajati SET IDSupervizor = NULL WHERE IDSupervizor = ?")) {
                updateStmt.setInt(1, sel.id);
                updateStmt.executeUpdate();
            }

            // SQL DELETE: Stergem angajatul
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM Angajati WHERE IDAngajat = ?")) {
                deleteStmt.setInt(1, sel.id);
                deleteStmt.executeUpdate();
            }

            conn.commit(); // Salvare modificari
            refreshAngajati();
            showInfo("Angajat sters cu succes! Subalternii au fost actualizati.");

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); } // Anulare in caz de eroare
            }
            showError("Nu se poate sterge! Verificati daca angajatul are utilizator asociat.");
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void handleUpdateAngajat() {
        AngajatModel sel = (angajatiTable != null) ? angajatiTable.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            showError("Selectati un angajat din tabel pentru modificare.");
            return;
        }

        String nume = tfAngNume.getText().trim();
        String prenume = tfAngPrenume.getText().trim();
        String functie = cbAngFunctie.getValue();
        String telefon = tfAngTelefon.getText().trim();
        String email = tfAngEmail.getText().trim();
        String salariuStr = tfAngSalariu.getText().trim();

        // Validare: campuri goale
        if (nume.isEmpty() || prenume.isEmpty() || functie == null || telefon.isEmpty() || email.isEmpty() || salariuStr.isEmpty()) {
            showError("Toate campurile sunt obligatorii!");
            return;
        }

        // Validare: format telefon (07xxxxxxxx)
        if (!telefon.matches("^07\\d{8}$")) {
            showError("Numar de telefon invalid! Trebuie sa inceapa cu '07' si sa aiba 10 cifre.");
            return;
        }

        // Validare: format email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format Email invalid! Exemplu: angajat@ballroom.ro");
            return;
        }

        double salariu;
        try {
            salariu = Double.parseDouble(salariuStr);
            if (salariu <= 0) {
                showError("Salariul trebuie sa fie mai mare decat 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Salariul trebuie sa fie un numar valid (ex: 3500.50).");
            return;
        }

        // Validare supervizor: nu poate fi el insusi
        AngajatModel supervisor = cbAngSupervisor.getValue();
        if (supervisor != null && supervisor.id == sel.id) {
            showError("Un angajat nu poate fi propriul supervizor.");
            return;
        }

        if (!showConfirmation("Sunteti sigur ca doriti sa modificati angajatul " + sel.nume + " " + sel.prenume + "?")) {
            return;
        }

        // query 42
        String sql = "UPDATE Angajati SET Nume = ?, Prenume = ?, Functie = ?, Telefon = ?, Email = ?, Salariu = ?, IDSupervizor = ? WHERE IDAngajat = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nume);
            pstmt.setString(2, prenume);
            pstmt.setString(3, functie);
            pstmt.setString(4, telefon);
            pstmt.setString(5, email);
            pstmt.setDouble(6, salariu);
            if (supervisor == null) pstmt.setObject(7, null);
            else pstmt.setInt(7, supervisor.id);
            pstmt.setInt(8, sel.id);

            pstmt.executeUpdate();
            refreshAngajati();
            clearAngajatiFields();
            showInfo("Angajat modificat cu succes!");

        } catch (SQLException e) {
            showError("Eroare la baza de date: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearAngajat() {
        clearAngajatiFields();
    }

    // Reset campuri angajat
    private void clearAngajatiFields() {
        tfAngNume.clear(); tfAngPrenume.clear(); tfAngTelefon.clear(); tfAngEmail.clear(); tfAngSalariu.clear();
        cbAngSupervisor.setValue(null);
    }

    // Configurare tabel Useri
    private void setupUsersTable() {
        colUserUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username));
        colUserNume.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nume));
        colUserPrenume.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().prenume));
        colUserRol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().rol));
        colUserAngajatNume.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().numeAngajatAsociat));

        setupAngajatComboBox(cbUserLinkAngajat);
        refreshUsers();
    }

    // Logica UI: Checkbox "User Extern" ==> Disable/Enable campuri
    private void setupUserCreationLogic() {
        chkUserExtern.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Extern ==> scriu manual nume/prenume
                cbUserLinkAngajat.setDisable(true);
                cbUserLinkAngajat.setValue(null);
                tfUserNume.setDisable(false);
                tfUserPrenume.setDisable(false);
                tfUserNume.clear();
                tfUserPrenume.clear();
            } else {
                // Angajat ==> aleg din lista
                cbUserLinkAngajat.setDisable(false);
                tfUserNume.setDisable(true);
                tfUserPrenume.setDisable(true);
            }
        });

        // Auto-complete: Select angajat ==> completare automata user/nume
        cbUserLinkAngajat.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !chkUserExtern.isSelected()) {
                tfUserNume.setText(newVal.nume);
                tfUserPrenume.setText(newVal.prenume);
                // Genereaza user: prenume.nume
                String generatedUser = (newVal.prenume + "." + newVal.nume).toLowerCase().replaceAll("\\s+", "");
                tfUserUser.setText(generatedUser);
            }
        });
    }

    // Incarcare Useri din DB
    private void refreshUsers() {
        ObservableList<UserModel> list = FXCollections.observableArrayList();
        // query 2 //// SQL: SELECT Useri + JOIN Angajati ==> sa vedem cine e userul in realitate
        String sql = "SELECT U.*, A.Nume AS AngNume, A.Prenume AS AngPrenume " +
                "FROM Utilizatori U " +
                "LEFT JOIN Angajati A ON U.IDAngajat = A.IDAngajat";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String angajatInfo = "-";
                if(rs.getInt("IDAngajat") != 0) {
                    angajatInfo = rs.getString("AngNume") + " " + rs.getString("AngPrenume");
                }

                list.add(new UserModel(
                        rs.getInt("IDUtilizator"), rs.getString("Username"), rs.getString("Nume"),
                        rs.getString("Prenume"), rs.getString("Rol"), rs.getInt("IDAngajat"), angajatInfo
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        usersTable.setItems(list);
    }

    // Buton Adauga User ==> Insert + Hashing parola
    @FXML
    private void handleAddUser() {
        String username = tfUserUser.getText().trim();
        String password = tfUserPass.getText();
        String nume = tfUserNume.getText().trim();
        String prenume = tfUserPrenume.getText().trim();
        String rol = cbUserRol.getValue();

        // Validari input
        if (username.isEmpty() || password.isEmpty() || rol == null) {
            showError("Username, Parola si Rolul sunt obligatorii!");
            return;
        }

        if (username.length() < 3) {
            showError("Username-ul trebuie sa aiba minim 3 caractere.");
            return;
        }

        // Validare specifica extern vs angajat
        if (chkUserExtern.isSelected()) {
            if (nume.isEmpty() || prenume.isEmpty()) {
                showError("Pentru utilizatori externi, Numele si Prenumele sunt obligatorii!");
                return;
            }
        } else {
            if (cbUserLinkAngajat.getValue() == null) {
                showError("Selectati un angajat din lista SAU bifati 'Utilizator Extern'.");
                return;
            }
        }

        if (!showConfirmation("Sunteti sigur ca doriti sa creati utilizatorul " + username + "?")) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             // SQL: INSERT ==> Creare user
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO Utilizatori (Username, ParolaHash, Nume, Prenume, Rol, IDAngajat) VALUES (?,?,?,?,?,?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt())); // Hashing parola
            pstmt.setString(3, nume);
            pstmt.setString(4, prenume);
            pstmt.setString(5, rol);

            if (chkUserExtern.isSelected()) {
                pstmt.setObject(6, null);
            } else {
                AngajatModel linkedAngajat = cbUserLinkAngajat.getValue();
                pstmt.setInt(6, linkedAngajat.id);
            }

            pstmt.executeUpdate();
            refreshUsers();

            tfUserUser.clear(); tfUserPass.clear(); tfUserNume.clear(); tfUserPrenume.clear();
            cbUserLinkAngajat.setValue(null); chkUserExtern.setSelected(false);
            showInfo("Utilizator creat cu succes!");

        } catch (Exception e) { showError("Eroare (probabil username deja existent): " + e.getMessage()); }
    }

    // Buton Sterge User
    @FXML
    private void handleDeleteUser() {
        UserModel sel = usersTable.getSelectionModel().getSelectedItem();
        if(sel == null) {
            showError("Selectati un utilizator pentru stergere.");
            return;
        }

        // Check: nu te poti sterge singur
        if (UserSession.getInstance() != null && sel.id == UserSession.getInstance().getUserId()) {
            showError("Nu va puteti sterge propriul cont in timp ce sunteti logat!");
            return;
        }

        if (!showConfirmation("Sunteti sigur ca doriti sa stergeti utilizatorul " + sel.username + "?")) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             // SQL: DELETE ==> Sterge user
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Utilizatori WHERE IDUtilizator = ?")) {
            pstmt.setInt(1, sel.id);
            pstmt.executeUpdate();
            refreshUsers();
            showInfo("Utilizator sters cu succes!");
        } catch (Exception e) { showError("Eroare la stergere: " + e.getMessage()); }
    }

    // Configurare tabel Logs
    private void setupLogsTable() {
        colLogUserTried.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().usernameTried));
        colLogDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date));
        colLogStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
        handleRefreshLogs();
    }

    // Incarcare Logs din DB
    @FXML
    private void handleRefreshLogs() {
        ObservableList<LogModel> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             // query 3 SQL: SELECT Logs DESC ==> Ultimele 50 de incercari de logare
             ResultSet rs = stmt.executeQuery("SELECT * FROM LogAcces ORDER BY DataLogin DESC LIMIT 50")) {
            while (rs.next()) {
                list.add(new LogModel(
                        rs.getInt("IDLog"), rs.getInt("IDUtilizator"), rs.getString("UsernameIncercat"),
                        rs.getTimestamp("DataLogin"), rs.getString("StatusLogin")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        logsTable.setItems(list);
    }

    // Afiseaza "Nume Prenume" in loc de obiect in ComboBox
    private void setupAngajatComboBox(ComboBox<AngajatModel> box) {
        box.setConverter(new StringConverter<AngajatModel>() {
            @Override
            public String toString(AngajatModel obj) {
                if (obj == null) return null;
                return obj.nume + " " + obj.prenume + " (" + obj.functie + ")";
            }

            @Override
            public AngajatModel fromString(String string) { return null; }
        });
    }

    // Pop-up Eroare
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare Validare");
        alert.setHeaderText("Problema intampinata");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Pop-up Succes
    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Pop-up Confirmare (DA/NU)
    private boolean showConfirmation(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }



    public static class AngajatModel {
        int id, idSupervizor;
        String nume, prenume, functie, telefon, email, numeSupervizor;
        double salariu;

        public AngajatModel(int id, String n, String p, String f, String t, String e, double s, int supId, String supName) {
            this.id=id; this.nume=n; this.prenume=p; this.functie=f; this.telefon=t;
            this.email=e; this.salariu=s; this.idSupervizor=supId; this.numeSupervizor = supName;
        }

        @Override
        public String toString() { return nume + " " + prenume; }
    }

    public static class UserModel {
        int id, idAngajat;
        String username, nume, prenume, rol, numeAngajatAsociat;

        public UserModel(int id, String u, String n, String p, String r, int ida, String angName) {
            this.id=id; this.username=u; this.nume=n; this.prenume=p; this.rol=r;
            this.idAngajat=ida; this.numeAngajatAsociat = angName;
        }
    }

    public static class LogModel {
        int id, userId; String usernameTried, date, status;
        public LogModel(int id, int uid, String userTry, Timestamp ts, String stat) {
            this.id=id; this.userId=uid; this.usernameTried=userTry;
            this.date=(ts!=null)?ts.toString():"-"; this.status=stat;
        }
    }

    public static class SalaAdminModel {
        int id;
        String denumire;
        int capacitate;
        double pretOra;
        boolean disponibil;

        public SalaAdminModel(int id, String denumire, int capacitate, double pretOra, boolean disponibil) {
            this.id = id;
            this.denumire = denumire;
            this.capacitate = capacitate;
            this.pretOra = pretOra;
            this.disponibil = disponibil;
        }
    }

    public static class ServiciuAdminModel {
        int id;
        String denumire;
        String descriere;
        String descriereScurta;
        double pret;

        public ServiciuAdminModel(int id, String denumire, String descriere, double pret) {
            this.id = id;
            this.denumire = denumire;
            this.descriere = descriere;
            this.pret = pret;

            if (descriere == null || descriere.isBlank()) {
                this.descriereScurta = "-";
            } else {
                String clean = descriere.replaceAll("\\s+", " ").trim();
                this.descriereScurta = clean.length() > 60 ? clean.substring(0, 60) + "..." : clean;
            }
        }
    }

    private static class SalaInput {
        String denumire;
        int capacitate;
        double pretOra;
        boolean disponibil;
        SalaInput(String denumire, int capacitate, double pretOra, boolean disponibil) {
            this.denumire = denumire;
            this.capacitate = capacitate;
            this.pretOra = pretOra;
            this.disponibil = disponibil;
        }
    }

    private static class ServiciuInput {
        String denumire;
        String descriere;
        double pret;
        ServiciuInput(String denumire, String descriere, double pret) {
            this.denumire = denumire;
            this.descriere = descriere;
            this.pret = pret;
        }
    }
}