package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.event.ActionEvent;
import database.DatabaseManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import model.EvenimentTabelModel;

/*
 * Controller pentru pagina de Gestiune Evenimente
   ==> tabelul principal, filtrele de cautare si butoanele de actiune
 */
public class EvenimenteController {

    // Filtrare
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Button searchButton;
    @FXML private Button clearButton;

    // Tabel + Coloane
    @FXML private TableView<EvenimentTabelModel> eventsTable;
    @FXML private TableColumn<EvenimentTabelModel, LocalDate> colData;
    @FXML private TableColumn<EvenimentTabelModel, String> colDenumire;
    @FXML private TableColumn<EvenimentTabelModel, String> colClient;
    @FXML private TableColumn<EvenimentTabelModel, String> colSala;
    @FXML private TableColumn<EvenimentTabelModel, String> colStatus;
    @FXML private TableColumn<EvenimentTabelModel, Double> colPret;
    // Coloane speciale cu butoane
    @FXML private TableColumn<EvenimentTabelModel, Void> colActiuni; // Buton pentru Angajati
    @FXML private TableColumn<EvenimentTabelModel, Void> colServicii; // Buton pentru Servicii

    // Adaugare, Editare, Stergere
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    // Lista care tine datele afisate in tabel
    private ObservableList<EvenimentTabelModel> listaEvenimente = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTypeComboBox();
        setupTableColumns();
        setupTableSelectionListener();
        setupActiuniColumn();
        setupServiciiColumn();
        refreshTable();
    }

    // Spunem fiecarei coloane ce proprietate din clasa EvenimentTabelModel sa afiseze
    private void setupTableColumns() {
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colDenumire.setCellValueFactory(new PropertyValueFactory<>("denumire"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("client"));
        colSala.setCellValueFactory(new PropertyValueFactory<>("sala"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPret.setCellValueFactory(new PropertyValueFactory<>("pretTotal"));
    }

    // configurare dropdown cu tipurile de evenimente la filtrare
    private void setupTypeComboBox() {
        ObservableList<String> types = FXCollections.observableArrayList(
                "Toate Tipurile", "Nunta", "Botez", "Corporate", "Altele");
        typeComboBox.setItems(types);
        typeComboBox.setValue("Toate Tipurile");

        Callback<ListView<String>, ListCell<String>> factory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        };
        typeComboBox.setCellFactory(factory);
        typeComboBox.setButtonCell(factory.call(null));
    }

    // activare butoanele de Editare/Stergere la selectia unui rand
    private void setupTableSelectionListener() {
        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean isSelected = (newSelection != null);
            btnEdit.setDisable(!isSelected);
            btnDelete.setDisable(!isSelected);
        });
    }

    // Cauta - da refresh la tabel cu filtrele aplicate
    @FXML
    private void handleSearch() {
        refreshTable();
    }

    // Reseteaza - sterge filtrele si reincarca tot
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        typeComboBox.setValue("Toate Tipurile");
        datePicker.setValue(null);
        refreshTable();
    }

    // Functia principala care aduce datele din baza de date
    // Construieste un SQL dinamic in functie de ce filtre sunt selectate
    private void refreshTable() {
        listaEvenimente.clear();

        List<Object> params = new ArrayList<>();

        // Incepem query-ul de baza cu JOIN-uri ca sa luam numele Clientului si al Salii
        // "WHERE 1=1" e un truc ca sa putem adauga conditii cu "AND" mai usor dupa
        // query 9
        StringBuilder sql = new StringBuilder(
                "SELECT E.IDEveniment, E.Data_Inceput, E.Denumire, CONCAT(C.Nume, ' ', C.Prenume) AS ClientNume, " +
                        "S.Denumire AS SalaNume, E.Status, E.Pret_Total " +
                        "FROM Evenimente AS E " +
                        "JOIN Clienti AS C ON E.IDClient = C.IDClient " +
                        "JOIN Sali AS S ON E.IDSala = S.IDSala " +
                        "WHERE 1=1"
        );

        // query 9.1 Filtru dupa Text
        String searchTerm = searchField.getText();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append(" AND (E.Denumire LIKE ? OR C.Nume LIKE ? OR C.Prenume LIKE ?)");
            String likeTerm = "%" + searchTerm + "%";
            params.add(likeTerm);
            params.add(likeTerm);
            params.add(likeTerm);
        }

        // query 9.2 Filtru dupa Tip Eveniment
        String eventType = typeComboBox.getValue();
        if (eventType != null && !eventType.equals("Toate Tipurile")) {
            switch (eventType) {
                case "Nunta":
                    sql.append(" AND E.Denumire LIKE ?");
                    params.add("%Nunta%");
                    break;
                case "Botez":
                    sql.append(" AND E.Denumire LIKE ?");
                    params.add("%Botez%");
                    break;
                case "Corporate":
                    // Cautam mai multe cuvinte cheie specifice corporate
                    sql.append(" AND (E.Denumire LIKE ? OR E.Denumire LIKE ? OR E.Denumire LIKE ? " +
                            "OR E.Denumire LIKE ? OR E.Denumire LIKE ? OR E.Denumire LIKE ? OR E.Denumire LIKE ?)");
                    params.add("%Corporate%");
                    params.add("%Conferinta%");
                    params.add("%Seminar%");
                    params.add("%Gala%");
                    params.add("%Lansare%");
                    params.add("%Team%");
                    params.add("%Workshop%");
                    break;
                case "Altele":
                    // Cautam petreceri diverse
                    sql.append(" AND (E.Denumire LIKE ? OR E.Denumire LIKE ? OR E.Denumire LIKE ? " +
                            "OR E.Denumire LIKE ? OR E.Denumire LIKE ?)");
                    params.add("%Petrecere%");
                    params.add("%Aniversare%");
                    params.add("%Balul%");
                    params.add("%Reuniune%");
                    params.add("%Revelion%");
                    break;
                default:
                    sql.append(" AND E.Denumire LIKE ?");
                    params.add("%" + eventType + "%");
                    break;
            }
        }

        // query 9.3 Filtru dupa Data exacta
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate != null) {
            sql.append(" AND E.Data_Inceput = ?");
            params.add(selectedDate);
        }

        // query 9.4 Ordonare descrescator dupa data
        sql.append(" ORDER BY E.Data_Inceput DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            // Punem parametrii in SQL
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    listaEvenimente.add(new EvenimentTabelModel(
                            rs.getInt("IDEveniment"),
                            rs.getDate("Data_Inceput").toLocalDate(),
                            rs.getString("Denumire"),
                            rs.getString("ClientNume"),
                            rs.getString("SalaNume"),
                            rs.getString("Status"),
                            rs.getDouble("Pret_Total")
                    ));
                }
            }

            eventsTable.setItems(listaEvenimente);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // sterge evenimentul selectat
    @FXML
    private void handleDeleteEvent() {
        EvenimentTabelModel selectedEvent = eventsTable.getSelectionModel().getSelectedItem();

        if (selectedEvent == null) {
            return;
        }

        // confirmare de la utilizator
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Stergere");
        alert.setHeaderText("Sunteti sigur ca doriti sa stergeti evenimentul?");
        alert.setContentText(selectedEvent.getDenumire() + " din data de " + selectedEvent.getData());

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM Evenimente WHERE IDEveniment = ?";

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, selectedEvent.getId());
                pstmt.executeUpdate();

                refreshTable();

            } catch (SQLException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Eroare SQL");
                errorAlert.setContentText("Nu s-a putut sterge evenimentul. Eroare: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    // Deschide formularul gol pentru adaugare
    @FXML
    private void handleAddEvent() {
        openEventForm(null);
    }

    // Deschide formularul precompletat pentru editare
    @FXML
    private void handleEditEvent() {
        EvenimentTabelModel selectedEvent = eventsTable.getSelectionModel().getSelectedItem();
        if (selectedEvent == null) return;

        openEventForm(selectedEvent.getId());
    }

    // deschide fereastra de formular
    private void openEventForm(Integer eventId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formulare/FormularEveniment.fxml"));
            Parent root = loader.load();

            FormularEvenimentController controller = loader.getController();
            controller.initData(eventId);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle(eventId == null ? "Adauga Eveniment Nou" : "Modifica Eveniment");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Dashboard/style.css").toExternalForm());
            popupStage.setScene(scene);

            popupStage.showAndWait();
            refreshTable(); // Refresh dupa ce inchidem formularul

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // configureaza coloana cu butonul "Vezi Angajati"
    private void setupActiuniColumn() {
        Callback<TableColumn<EvenimentTabelModel, Void>, TableCell<EvenimentTabelModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<EvenimentTabelModel, Void> call(final TableColumn<EvenimentTabelModel, Void> param) {

                        final TableCell<EvenimentTabelModel, Void> cell = new TableCell<>() {

                            private final Button viewButton = new Button("Vezi");
                            private final HBox pane = new HBox(viewButton);

                            {
                                pane.setAlignment(Pos.CENTER);
                                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");
                                viewButton.setOnAction((ActionEvent e) -> {
                                    EvenimentTabelModel event = getTableView().getItems().get(getIndex());
                                    openAngajatiPopup(event.getId(), event.getDenumire());
                                });
                            }

                            @Override
                            public void updateItem(Void item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                } else {
                                    setGraphic(pane);
                                }
                            }
                        };
                        return cell;
                    }
                };

        colActiuni.setCellFactory(cellFactory);
    }

    // deschide fereastra cu lista de angajati alocati
    private void openAngajatiPopup(int eventId, String eventName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formulare/VizualizareAngajati.fxml"));
            Parent root = loader.load();

            VizualizareAngajatiController controller = loader.getController();
            controller.initData(eventId, eventName);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Personal Alocat");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Dashboard/style.css").toExternalForm());
            popupStage.setScene(scene);

            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // configureaza coloana cu butonul "Vezi Servicii"
    private void setupServiciiColumn() {
        Callback<TableColumn<EvenimentTabelModel, Void>, TableCell<EvenimentTabelModel, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<EvenimentTabelModel, Void> call(final TableColumn<EvenimentTabelModel, Void> param) {

                        final TableCell<EvenimentTabelModel, Void> cell = new TableCell<>() {

                            private final Button viewButton = new Button("Vezi");
                            private final HBox pane = new HBox(viewButton);

                            {
                                pane.setAlignment(Pos.CENTER);
                                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");

                                viewButton.setOnAction((ActionEvent e) -> {
                                    EvenimentTabelModel event = getTableView().getItems().get(getIndex());
                                    openServiciiPopup(event.getId(), event.getDenumire());
                                });
                            }

                            @Override
                            public void updateItem(Void item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                } else {
                                    setGraphic(pane);
                                }
                            }
                        };
                        return cell;
                    }
                };

        colServicii.setCellFactory(cellFactory);
    }

    // deschide fereastra cu lista de servicii alocate
    private void openServiciiPopup(int eventId, String eventName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formulare/VizualizareServicii.fxml"));
            Parent root = loader.load();

            VizualizareServiciiController controller = loader.getController();
            controller.initData(eventId, eventName);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Servicii Alocate");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Dashboard/style.css").toExternalForm());
            popupStage.setScene(scene);

            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}