package controller;

import database.DatabaseManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import javafx.event.ActionEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Controller pentru Dashboard ==> afisam statistici, grafice si urmatoarele evenimente
 */
public class DashboardController {

    // Legaturi cu elementele din interfata grafica
    @FXML private Label kpiActiveEvents;
    @FXML private Label kpiEventsMonth;
    @FXML private Label kpiTotalClients;
    @FXML private TableView<EventModel> eventsTable;
    @FXML private TableColumn<EventModel, String> colEventData;
    @FXML private TableColumn<EventModel, String> colEventNume;
    @FXML private TableColumn<EventModel, String> colEventClient;
    @FXML private TableColumn<EventModel, String> colEventSala;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private CategoryAxis xAxis;
    @FXML private Button btnContactClienti;
    @FXML private Button btnContactSupervizori;

    // numele lunilor pt axa X a graficului
    private final ObservableList<String> monthNames = FXCollections.observableArrayList(
            "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi", "Dec"
    );

    // Functia care porneste automat cand intram in aplicatie
    @FXML
    public void initialize() {
        // Configuram graficul si incarcam datele
        xAxis.setCategories(monthNames);
        revenueChart.setCategoryGap(10);
        revenueChart.setBarGap(0);
        loadKpiCards();
        setupEventsTable();
        loadEventsTableData();
        loadRevenueChartData();
    }

    private void loadKpiCards() {
        // query 5 SQL SELECT: Numara cate evenimente sunt planificate in total
        String activeEventsSql = "SELECT COUNT(IDEveniment) FROM Evenimente WHERE Status = 'Planificat'";

        // query 6 SQL SELECT: Numara evenimentele din luna si anul curent
        String eventsMonthSql = "SELECT COUNT(IDEveniment) FROM Evenimente WHERE MONTH(Data_Inceput) = MONTH(CURDATE()) AND YEAR(Data_Inceput) = YEAR(CURDATE())";

        // query 7 SQL SELECT: Numara cati clienti avem in total
        String totalClientsSql = "SELECT COUNT(IDClient) FROM Clienti";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Executam interogarile si punem rezultatele in etichete
            ResultSet rs1 = stmt.executeQuery(activeEventsSql);
            if (rs1.next()) kpiActiveEvents.setText(rs1.getString(1));

            ResultSet rs2 = stmt.executeQuery(eventsMonthSql);
            if (rs2.next()) kpiEventsMonth.setText(rs2.getString(1));

            ResultSet rs3 = stmt.executeQuery(totalClientsSql);
            if (rs3.next()) kpiTotalClients.setText(rs3.getString(1));

        } catch (SQLException e) {
            e.printStackTrace();
            kpiActiveEvents.setText("Eroare");
        }
    }

    // setup tabel
    private void setupEventsTable() {
        colEventData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colEventNume.setCellValueFactory(new PropertyValueFactory<>("numeEveniment"));
        colEventClient.setCellValueFactory(new PropertyValueFactory<>("numeClient"));
        colEventSala.setCellValueFactory(new PropertyValueFactory<>("numeSala"));
    }

    // din db se iau urmatoarele evenimente si le punem in tabel
    private void loadEventsTableData() {
        ObservableList<EventModel> eventsList = FXCollections.observableArrayList();

        // query 8 SQL SELECT: Iau data, numele evenimentului, numele clientului si sala pentru urmatoarele 10 evenimente planificate
        String sql = "SELECT E.Data_Inceput, E.Denumire, CONCAT(C.Nume, ' ', C.Prenume) AS ClientNume, S.Denumire AS SalaNume " +
                "FROM Evenimente AS E " +
                "JOIN Clienti AS C ON E.IDClient = C.IDClient " +
                "JOIN Sali AS S ON E.IDSala = S.IDSala " +
                "WHERE E.Data_Inceput >= CURDATE() AND E.Status = 'Planificat' " +
                "ORDER BY E.Data_Inceput ASC " +
                "LIMIT 10";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
            while (rs.next()) {
                LocalDate date = rs.getDate("Data_Inceput").toLocalDate();
                eventsList.add(new EventModel(
                        date.format(dtf),
                        rs.getString("Denumire"),
                        rs.getString("ClientNume"),
                        rs.getString("SalaNume")
                ));
            }
            eventsTable.setItems(eventsList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRevenueChartData() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::loadRevenueChartData);
            return;
        }

        revenueChart.getData().clear();

        if (revenueChart.getXAxis() instanceof CategoryAxis) {
            ((CategoryAxis) revenueChart.getXAxis()).getCategories().clear();
        }

        if (revenueChart.getYAxis() instanceof NumberAxis) {
            ((NumberAxis) revenueChart.getYAxis()).setAutoRanging(true);
        }

        // query 24
        String sql =
                "SELECT DATE_FORMAT(Data_Inceput, '%Y-%m') AS Luna, " +
                        "       SUM(Pret_Total) AS Venit_Total " +
                        "FROM Evenimente " +
                        "WHERE Data_Inceput >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                        "  AND Status = 'Finalizat' " +
                        "GROUP BY DATE_FORMAT(Data_Inceput, '%Y-%m') " +
                        "ORDER BY Luna";

        // ultimele 12 luni (inclusiv luna curenta)
        YearMonth start = YearMonth.now().minusMonths(11);
        List<String> last12 = new ArrayList<>();
        Map<String, Double> venitByMonth = new HashMap<>();

        for (int i = 0; i < 12; i++) {
            String key = start.plusMonths(i).toString();
            last12.add(key);
            venitByMonth.put(key, 0.0);
        }

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String luna = rs.getString("Luna");
                BigDecimal bd = rs.getBigDecimal("Venit_Total");
                double venit = (bd != null) ? bd.doubleValue() : 0.0;

                if (venitByMonth.containsKey(luna)) {
                    venitByMonth.put(luna, venit);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (revenueChart.getXAxis() instanceof CategoryAxis) {
            ((CategoryAxis) revenueChart.getXAxis()).getCategories().setAll(last12);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Venituri ultimele 12 luni");

        for (String luna : last12) {
            series.getData().add(new XYChart.Data<>(luna, venitByMonth.get(luna)));
        }

        revenueChart.getData().add(series);
    }

    // tine datele unui rand din tabel
    public static class EventModel {
        private final String data;
        private final String numeEveniment;
        private final String numeClient;
        private final String numeSala;

        public EventModel(String data, String numeEveniment, String numeClient, String numeSala) {
            this.data = data;
            this.numeEveniment = numeEveniment;
            this.numeClient = numeClient;
            this.numeSala = numeSala;
        }

        public String getData() { return data; }
        public String getNumeEveniment() { return numeEveniment; }
        public String getNumeClient() { return numeClient; }
        public String getNumeSala() { return numeSala; }
    }

    // Deschide fereastra de adaugare eveniment
    @FXML
    private void handleAddEvent(ActionEvent event) {
        openPopup("/Formulare/FormularEveniment.fxml", "Adauga Eveniment Nou");
        // refresh la dashboard dupa ce inchidem fereastra
        initialize();
    }

    // Deschide fereastra de adaugare client
    @FXML
    private void handleAddClient(ActionEvent event) {
        openPopup("/Formulare/FormularClient.fxml", "Adauga Client Nou");
        loadKpiCards();
    }

    // Functie universala pentru a deschide ferestre noi (pop-up)
    private void openPopup(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle(title);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Dashboard/style.css").toExternalForm());
            popupStage.setScene(scene);

            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deschide lista de contacte pentru clienti
    @FXML
    private void handleContactClienti(ActionEvent event) {
        openContactPopup("clienti");
    }

    // Deschide lista de contacte pentru sefi
    @FXML
    private void handleContactSupervizori(ActionEvent event) {
        openContactPopup("supervizori");
    }

    // Functie speciala pentru a deschide fereastra de contacte
    private void openContactPopup(String contactType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formulare/ContactList.fxml"));
            Parent root = loader.load();

            // Trimitem tipul de contact catre controller
            ContactListController controller = loader.getController();
            controller.initData(contactType);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Lista Contacte");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Dashboard/style.css").toExternalForm());
            popupStage.setScene(scene);

            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}