package io.github.ordonovus.sdmxconverter.presentation.dialog;

import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallationManager;
import io.github.ordonovus.sdmxconverter.presentation.controller.ConverterManagementController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * Creates and displays the application settings window.
 */
public final class SettingsDialog {

    private static final double MINIMUM_WIDTH = 760;
    private static final double MINIMUM_HEIGHT = 520;
    private static final double SCREEN_MARGIN = 24;

    private static final String VIEW_RESOURCE =
            "/view/settings-view.fxml";

    private static final String STYLESHEET_RESOURCE =
            "/css/application.css";

    /**
     * Opens the application settings window.
     *
     * @param owner parent application window
     * @param installationManager shared converter installation manager
     * @throws IOException if the FXML view cannot be loaded
     */
    public void show(
            Window owner,
            ConverterInstallationManager installationManager
    ) throws IOException {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(
                installationManager,
                "installationManager"
        );

        FXMLLoader loader = new FXMLLoader(
                requireResource(VIEW_RESOURCE)
        );

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                requireResource(STYLESHEET_RESOURCE)
                        .toExternalForm()
        );

        ConverterManagementController controller =
                loader.getController();

        controller.configure(installationManager);

        Stage dialog = createDialog(owner, scene);

        dialog.sizeToScene();
        fitToAvailableScreen(dialog, owner);
        centerOnOwner(dialog, owner);

        dialog.showAndWait();
    }

    private Stage createDialog(
            Window owner,
            Scene scene
    ) {
        Stage dialog = new Stage();

        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Configuración");
        dialog.setScene(scene);

        if (owner instanceof Stage ownerStage) {
            dialog.getIcons().setAll(ownerStage.getIcons());
        }

        dialog.setResizable(false);

        return dialog;
    }

    private void fitToAvailableScreen(
            Stage dialog,
            Window owner
    ) {
        Rectangle2D visualBounds =
                findOwnerScreen(owner).getVisualBounds();

        double maximumWidth = Math.max(
                visualBounds.getWidth() - SCREEN_MARGIN * 2,
                1
        );

        double maximumHeight = Math.max(
                visualBounds.getHeight() - SCREEN_MARGIN * 2,
                1
        );

        double minimumWidth = Math.min(
                MINIMUM_WIDTH,
                maximumWidth
        );

        double minimumHeight = Math.min(
                MINIMUM_HEIGHT,
                maximumHeight
        );

        dialog.setMinWidth(minimumWidth);
        dialog.setMinHeight(minimumHeight);
        dialog.setMaxWidth(maximumWidth);
        dialog.setMaxHeight(maximumHeight);

        dialog.setWidth(
                limit(
                        dialog.getWidth(),
                        minimumWidth,
                        maximumWidth
                )
        );

        dialog.setHeight(
                limit(
                        dialog.getHeight(),
                        minimumHeight,
                        maximumHeight
                )
        );
    }

    private void centerOnOwner(
            Stage dialog,
            Window owner
    ) {
        Rectangle2D visualBounds =
                findOwnerScreen(owner).getVisualBounds();

        double centeredX = owner.getX()
                + (owner.getWidth() - dialog.getWidth()) / 2;

        double centeredY = owner.getY()
                + (owner.getHeight() - dialog.getHeight()) / 2;

        double maximumX = visualBounds.getMaxX()
                - dialog.getWidth();

        double maximumY = visualBounds.getMaxY()
                - dialog.getHeight();

        dialog.setX(
                limit(
                        centeredX,
                        visualBounds.getMinX(),
                        maximumX
                )
        );

        dialog.setY(
                limit(
                        centeredY,
                        visualBounds.getMinY(),
                        maximumY
                )
        );
    }

    private Screen findOwnerScreen(Window owner) {
        List<Screen> screens = Screen.getScreensForRectangle(
                owner.getX(),
                owner.getY(),
                owner.getWidth(),
                owner.getHeight()
        );

        return screens.isEmpty()
                ? Screen.getPrimary()
                : screens.getFirst();
    }

    private double limit(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(value, maximum)
        );
    }

    private URL requireResource(String resourcePath) {
        return Objects.requireNonNull(
                SettingsDialog.class.getResource(resourcePath),
                "Resource was not found: " + resourcePath
        );
    }

}