package ru.davidlevy.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClassServer extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("controller.fxml"));
        primaryStage.setTitle("Chatter-server");
        primaryStage.toFront();
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setScene(new Scene(root, 800, 400));
        primaryStage.show();
    }
}
