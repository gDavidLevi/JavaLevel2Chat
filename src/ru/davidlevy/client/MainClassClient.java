package ru.davidlevy.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClassClient extends Application {
    static Stage mainStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;
        primaryStage.setTitle("Chatter-client");
        //
        primaryStage.toFront();
        primaryStage.centerOnScreen();
        primaryStage.setResizable(false);
        Parent root = FXMLLoader.load(getClass().getResource("controller.fxml"));
        Scene scene = new Scene(root, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> Controller.disconnect());
    }
}
