package com.screenrecorder;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Chubby Recorder Application
 * A silent screen recorder with live preview, audio capture, and various output options
 */
public class ScreenRecorderApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ScreenRecorderApp.class.getResource("/view/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        // Set application icon
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/asset/icon.png")));
        
        stage.setTitle("Chubby Recorder");
        stage.setScene(scene);
        stage.setMinWidth(750);
        stage.setMinHeight(550);
        stage.show();
        
        // Ensure clean shutdown when window is closed
        stage.setOnCloseRequest(event -> {
            ScreenRecorderController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.shutdown();
            }
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
