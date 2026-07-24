package com.maple.launcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.InputStream;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MapleLauncher");

        // Set Window & Taskbar Icon from src/main/resources/icon.png
        InputStream iconStream = getClass().getResourceAsStream("/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }

        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 500);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
