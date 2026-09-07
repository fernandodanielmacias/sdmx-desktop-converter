package io.github.ordonovus.sdmxconverter.presentation.controller;

import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallationManager;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallationValidationResult;
import io.github.ordonovus.sdmxconverter.presentation.dialog.FileDialogService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Controls the window used to inspect and manage the SDMX Converter
 * installation.
 */
public final class ConverterManagementController {

    private static final String STATUS_STYLE_CLASS =
            "converter-validation-status";

    private static final String SUCCESS_STYLE_CLASS =
            "converter-validation-success";

    private static final String WARNING_STYLE_CLASS =
            "converter-validation-warning";

    private static final String ERROR_STYLE_CLASS =
            "converter-validation-error";

    private final FileDialogService fileDialogService =
            new FileDialogService();

    private ConverterInstallationManager installationManager;

    @FXML
    private Label converterVersionValueLabel;

    @FXML
    private Label requiredJavaValueLabel;

    @FXML
    private TextField converterDirectoryField;

    @FXML
    private TextField javaExecutableField;

    @FXML
    private Label validationStatusLabel;

    @FXML
    private VBox validationMessagesBox;

    @FXML
    private ProgressIndicator validationProgressIndicator;

    @FXML
    private Button selectInstallationButton;

    @FXML
    private Button validateInstallationButton;

    @FXML
    private Button restoreInstallationButton;

    /**
     * Configures the initial state of the converter management window.
     */
    @FXML
    private void initialize() {
        validationProgressIndicator.setVisible(false);
        validationProgressIndicator.setManaged(false);
    }

    /**
     * Supplies the converter installation manager shared by the main
     * application.
     *
     * @param installationManager converter installation manager
     */
    public void configure(
            ConverterInstallationManager installationManager
    ) {
        this.installationManager = Objects.requireNonNull(
                installationManager,
                "installationManager"
        );

        displayInstallation(
                installationManager.getActiveInstallation()
        );

        validateActiveInstallation();
    }

    /**
     * Opens a directory chooser to locate another compatible Converter
     * installation.
     */
    @FXML
    private void onSelectInstallation() {
        requireConfiguredManager();

        ConverterInstallation activeInstallation =
                installationManager.getActiveInstallation();

        var selectedDirectory =
                fileDialogService.chooseConverterDirectory(
                        getWindow(),
                        activeInstallation.installationDirectory()
                );

        if (selectedDirectory.isEmpty()) {
            return;
        }

        Path converterDirectory = selectedDirectory.get();

        converterDirectoryField.setText(
                converterDirectory.toString()
        );

        runValidation(
                "Validando la instalación seleccionada...",
                () -> installationManager.selectInstallation(
                        converterDirectory
                ),
                converterDirectory
        );
    }

    /**
     * Validates the currently active Converter installation.
     */
    @FXML
    private void onValidateInstallation() {
        requireConfiguredManager();
        validateActiveInstallation();
    }

    /**
     * Restores and validates the default Converter installation.
     */
    @FXML
    private void onRestoreInstallation() {
        requireConfiguredManager();

        runValidation(
                "Restaurando la instalación predeterminada...",
                () -> {
                    installationManager.restoreDefaultInstallation();

                    return installationManager
                            .validateActiveInstallation();
                },
                null
        );
    }

    /**
     * Closes the converter management window.
     */
    @FXML
    private void onClose() {
        Window window = getWindow();

        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void validateActiveInstallation() {
        runValidation(
                "Validando la instalación activa...",
                installationManager::validateActiveInstallation,
                null
        );
    }

    private void runValidation(
            String progressMessage,
            Supplier<ConverterInstallationValidationResult> operation,
            Path attemptedDirectory
    ) {
        setValidationRunning(true);
        clearValidationMessages();
        updateValidationStatus(
                progressMessage,
                STATUS_STYLE_CLASS
        );

        Task<ConverterInstallationValidationResult> task =
                new Task<>() {

                    @Override
                    protected ConverterInstallationValidationResult call() {
                        return operation.get();
                    }
                };

        task.setOnSucceeded(event -> {
            setValidationRunning(false);

            ConverterInstallationValidationResult result =
                    task.getValue();

            if (result.isValid()) {
                displayInstallation(
                        installationManager.getActiveInstallation()
                );
            } else if (attemptedDirectory != null) {
                converterDirectoryField.setText(
                        attemptedDirectory.toString()
                );
            }

            displayValidationResult(result);
        });

        task.setOnFailed(event -> {
            setValidationRunning(false);

            Throwable exception = task.getException();
            String detail = exception == null
                    ? "No se pudo determinar la causa."
                    : exception.getMessage();

            updateValidationStatus(
                    "No fue posible validar el Convertidor SDMX.",
                    ERROR_STYLE_CLASS
            );

            addValidationMessage(
                    "Detalle: " + detail,
                    ERROR_STYLE_CLASS
            );
        });

        Thread validationThread = new Thread(
                task,
                "converter-installation-validation"
        );
        validationThread.setDaemon(true);
        validationThread.start();
    }

    private void displayInstallation(
            ConverterInstallation installation
    ) {
        converterVersionValueLabel.setText(
                installation.converterVersion()
        );

        requiredJavaValueLabel.setText(
                "Java " + installation.requiredJavaMajorVersion()
        );

        converterDirectoryField.setText(
                installation.installationDirectory().toString()
        );

        javaExecutableField.setText(
                installation.javaExecutable().toString()
        );
    }

    private void displayValidationResult(
            ConverterInstallationValidationResult result
    ) {
        clearValidationMessages();

        for (String error : result.errors()) {
            addValidationMessage(
                    error,
                    ERROR_STYLE_CLASS
            );
        }

        for (String warning : result.warnings()) {
            addValidationMessage(
                    warning,
                    WARNING_STYLE_CLASS
            );
        }

        if (!result.isValid()) {
            updateValidationStatus(
                    "La instalación no es compatible y no fue activada.",
                    ERROR_STYLE_CLASS
            );
            return;
        }

        if (result.hasWarnings()) {
            updateValidationStatus(
                    "La instalación es compatible, pero requiere atención.",
                    WARNING_STYLE_CLASS
            );
            return;
        }

        updateValidationStatus(
                "El Convertidor SDMX está configurado correctamente.",
                SUCCESS_STYLE_CLASS
        );

        addValidationMessage(
                "La instalación está lista para realizar conversiones.",
                SUCCESS_STYLE_CLASS
        );
    }

    private void addValidationMessage(
            String message,
            String styleClass
    ) {
        var label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().add(
                "converter-validation-message"
        );
        label.getStyleClass().add(styleClass);

        validationMessagesBox.getChildren().add(label);
    }

    private void clearValidationMessages() {
        validationMessagesBox.getChildren().clear();
    }

    private void updateValidationStatus(
            String message,
            String styleClass
    ) {
        validationStatusLabel.setText(message);
        validationStatusLabel.getStyleClass().setAll(
                STATUS_STYLE_CLASS,
                styleClass
        );
    }

    private void setValidationRunning(boolean running) {
        validationProgressIndicator.setVisible(running);
        validationProgressIndicator.setManaged(running);

        selectInstallationButton.setDisable(running);
        validateInstallationButton.setDisable(running);
        restoreInstallationButton.setDisable(running);
    }

    private void requireConfiguredManager() {
        if (installationManager == null) {
            throw new IllegalStateException(
                    "Converter installation manager was not configured"
            );
        }
    }

    private Window getWindow() {
        return converterDirectoryField.getScene().getWindow();
    }

}