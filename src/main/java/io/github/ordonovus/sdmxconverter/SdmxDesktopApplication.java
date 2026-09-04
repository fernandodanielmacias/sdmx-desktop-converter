package io.github.ordonovus.sdmxconverter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Entry point for the SDMX Desktop Converter application.
 */
public final class SdmxDesktopApplication extends Application {

    private static final double INITIAL_WIDTH = 1_180;
    private static final double INITIAL_HEIGHT = 760;
    private static final double MINIMUM_WIDTH = 940;
    private static final double MINIMUM_HEIGHT = 620;

    /**
     * Loads and displays the primary application window.
     *
     * @param stage primary JavaFX stage
     * @throws IOException when the main FXML resource cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        var loader = new FXMLLoader(
                SdmxDesktopApplication.class.getResource(
                        "/view/main-view.fxml"
                )
        );

        var scene = new Scene(
                loader.load(),
                INITIAL_WIDTH,
                INITIAL_HEIGHT
        );

        String stylesheet = Objects.requireNonNull(
                SdmxDesktopApplication.class.getResource(
                        "/css/application.css"
                ),
                "Application stylesheet was not found"
        ).toExternalForm();

        scene.getStylesheets().add(stylesheet);

        stage.setTitle("Convertidor SDMX");
        stage.setMinWidth(MINIMUM_WIDTH);
        stage.setMinHeight(MINIMUM_HEIGHT);
        stage.setScene(scene);

        String icon = Objects.requireNonNull(
                SdmxDesktopApplication.class.getResource(
                        "/images/application-icon.png"
                ),
                "Application icon was not found"
        ).toExternalForm();

        stage.getIcons().add(new Image(icon));
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