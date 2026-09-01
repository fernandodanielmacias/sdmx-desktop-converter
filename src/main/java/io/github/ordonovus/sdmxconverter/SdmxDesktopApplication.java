package io.github.ordonovus.sdmxconverter;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Entry point for the SDMX Desktop Converter application.
 */
public final class SdmxDesktopApplication extends Application {

    private static final double INITIAL_WIDTH = 960;
    private static final double INITIAL_HEIGHT = 640;

    /**
     * Initializes and displays the primary application window.
     *
     * @param stage primary JavaFX stage
     */
    @Override
    public void start(Stage stage) {
        var titleLabel = new Label("SDMX Desktop Converter");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        var statusLabel = new Label(
                "JavaFX configurado correctamente."
        );

        var root = new VBox(16, titleLabel, statusLabel);
        root.setPadding(new Insets(32));

        var scene = new Scene(
                root,
                INITIAL_WIDTH,
                INITIAL_HEIGHT
        );

        stage.setTitle("SDMX Desktop Converter");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Starts the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}