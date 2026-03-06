package main;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {
    private static HostServices hostServices;

    @Override
    public void start(Stage primaryStage) {
        hostServices = getHostServices();

        try {
            URL fxmlUrl = getClass().getResource("/Login/Login.fxml");
            URL cssUrl = getClass().getResource("/Login/style.css");

            if (fxmlUrl == null || cssUrl == null) {
                System.err.println("Eroare: Nu gasesc fisierele FXML sau CSS!");
                System.err.println("Verifica ca 'Login.fxml' si 'style.css' sunt in src/main/resources/Login/");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(cssUrl.toExternalForm());

            primaryStage.setTitle("Ballroom Event Management - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HostServices getStaticHostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
