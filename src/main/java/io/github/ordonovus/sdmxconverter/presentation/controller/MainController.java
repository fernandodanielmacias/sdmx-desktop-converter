package io.github.ordonovus.sdmxconverter.presentation.controller;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionParameters;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequestValidator;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionService;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallationManager;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallationProvider;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallationValidator;
import io.github.ordonovus.sdmxconverter.application.service.OutputFileNameService;
import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import io.github.ordonovus.sdmxconverter.infrastructure.converter.SdmxConverterProcessExecutor;
import io.github.ordonovus.sdmxconverter.infrastructure.desktop.DesktopDirectoryOpener;
import io.github.ordonovus.sdmxconverter.infrastructure.sdmx.DsdMetadataReader;
import io.github.ordonovus.sdmxconverter.infrastructure.xml.SdmxXmlValidator;
import io.github.ordonovus.sdmxconverter.presentation.cell.ConversionStatusTableCell;
import io.github.ordonovus.sdmxconverter.presentation.conversion.ConversionWorkflowManager;
import io.github.ordonovus.sdmxconverter.presentation.conversion.ExistingOutputCoordinator;
import io.github.ordonovus.sdmxconverter.presentation.dialog.ExistingOutputDialog;
import io.github.ordonovus.sdmxconverter.presentation.dialog.SettingsDialog;
import io.github.ordonovus.sdmxconverter.presentation.dialog.FileDialogService;
import io.github.ordonovus.sdmxconverter.presentation.factory.ConversionQueueFactory;
import io.github.ordonovus.sdmxconverter.presentation.log.ActivityLogFileExporter;
import io.github.ordonovus.sdmxconverter.presentation.log.ActivityLogManager;
import io.github.ordonovus.sdmxconverter.presentation.log.ConversionDiagnosticManager;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogEntry;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogLevel;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionFileRow;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionQueueItem;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

/**
 * Coordinates the main conversion screen and delegates specialized behavior
 * to application and presentation services.
 */
public final class MainController {

    private static final String CONVERTER_VERSION = "11.8.1";

    private final FileDialogService fileDialogService =
            new FileDialogService();

    private final OutputFileNameService outputFileNameService =
            new OutputFileNameService();

    private final DsdMetadataReader dsdMetadataReader =
            new DsdMetadataReader();

    private final ConverterInstallationManager converterInstallationManager =
            new ConverterInstallationManager(
                    new ConverterInstallationProvider(),
                    new ConverterInstallationValidator()
            );

    private final SettingsDialog settingsDialog =
            new SettingsDialog();

    private final ConversionQueueFactory conversionQueueFactory =
            new ConversionQueueFactory();

    private final SdmxConversionService conversionService =
            new SdmxConversionService(
                    new SdmxConversionRequestValidator(),
                    new SdmxConverterProcessExecutor(),
                    new SdmxXmlValidator()
            );

    private final DesktopDirectoryOpener desktopDirectoryOpener =
            new DesktopDirectoryOpener();

    private final ActivityLogFileExporter activityLogFileExporter =
            new ActivityLogFileExporter();

    private final ConversionDiagnosticManager conversionDiagnosticManager =
            new ConversionDiagnosticManager();

    private final SdmxConversionParameters conversionParameters =
            SdmxConversionParameters.defaults();

    private ConversionWorkflowManager conversionWorkflowManager;

    private ActivityLogManager activityLogManager;

    private ExistingOutputCoordinator existingOutputCoordinator;

    private Path selectedDsdFile;
    private DsdMetadata selectedDsdMetadata;
    private Path selectedHeaderFile;

    @FXML
    private TableView<ConversionFileRow> filesTable;

    @FXML
    private TableColumn<ConversionFileRow, String> fileNameColumn;

    @FXML
    private TableColumn<ConversionFileRow, String> outputFileNameColumn;

    @FXML
    private TableColumn<ConversionFileRow, String> filePathColumn;

    @FXML
    private TableColumn<ConversionFileRow, String> statusColumn;

    @FXML
    private TableColumn<ConversionFileRow, String> seriesCountColumn;

    @FXML
    private TableColumn<ConversionFileRow, String> observationCountColumn;

    @FXML
    private TextField outputDirectoryField;

    @FXML
    private TextField dsdFileField;

    @FXML
    private TextField headerFileField;

    @FXML
    private Label dsdAgencyValueLabel;

    @FXML
    private Label dsdIdValueLabel;

    @FXML
    private Label dsdVersionValueLabel;

    @FXML
    private Label configurationSummaryLabel;

    @FXML
    private Label selectionCountLabel;

    @FXML
    private Label generalStatusLabel;

    @FXML
    private Button convertButton;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private VBox activityLogPanel;

    @FXML
    private ListView<ActivityLogEntry> activityLogList;

    @FXML
    private Button toggleActivityLogButton;

    @FXML
    private VBox conversionProgressContainer;

    @FXML
    private ProgressBar conversionProgressBar;

    @FXML
    private Label conversionProgressLabel;

    @FXML
    private Label conversionProgressPercentageLabel;

    @FXML
    private Button cancelConversionButton;

    @FXML
    private Button removeSelectedFilesButton;

    @FXML
    private Button clearFilesButton;

    @FXML
    private Button chooseFilesButton;

    @FXML
    private Button chooseOutputDirectoryButton;

    @FXML
    private Button openOutputDirectoryButton;

    @FXML
    private Button chooseDsdFileButton;

    @FXML
    private Button chooseHeaderFileButton;

    @FXML
    private Button resetFormButton;

    /**
     * Configures the screen after all FXML elements have been injected.
     */
    @FXML
    private void initialize() {
        configureTable();

        activityLogManager = new ActivityLogManager(
                activityLogList,
                activityLogPanel,
                generalStatusLabel,
                toggleActivityLogButton,
                contentScrollPane
        );

        existingOutputCoordinator =
                new ExistingOutputCoordinator(
                        new ExistingOutputDialog(),
                        activityLogManager
                );

        conversionWorkflowManager =
                new ConversionWorkflowManager(
                        activityLogManager,
                        conversionProgressContainer,
                        conversionProgressBar,
                        conversionProgressLabel,
                        conversionProgressPercentageLabel,
                        cancelConversionButton,
                        filesTable,
                        List.of(
                                removeSelectedFilesButton,
                                clearFilesButton,
                                chooseFilesButton,
                                chooseOutputDirectoryButton,
                                chooseDsdFileButton,
                                chooseHeaderFileButton,
                                resetFormButton
                        )
                );

        convertButton.disableProperty().bind(
                Bindings.isEmpty(filesTable.getItems())
                        .or(outputDirectoryField.textProperty().isEmpty())
                        .or(dsdFileField.textProperty().isEmpty())
                        .or(headerFileField.textProperty().isEmpty())
                        .or(conversionWorkflowManager.runningProperty())
        );

        openOutputDirectoryButton.disableProperty().bind(
                outputDirectoryField.textProperty().isEmpty()
        );

        updateSelectionCount();
        updateConfigurationSummary();

        activityLogManager.add(
                ActivityLogLevel.INFORMATION,
                "Aplicación lista para seleccionar archivos."
        );
    }

    /**
     * Opens a multiple-selection dialog for Excel input files.
     */
    @FXML
    private void onChooseFiles() {
        List<Path> selectedFiles =
                fileDialogService.chooseExcelFiles(
                        getWindow(),
                        getPreferredInitialLocation(null)
                );

        if (selectedFiles.isEmpty()) {
            return;
        }

        Set<Path> existingPaths = new HashSet<>();

        for (ConversionFileRow row : filesTable.getItems()) {
            existingPaths.add(row.getPath());
        }

        List<String> existingOutputFileNames =
                getOutputFileNames(null);

        int addedFiles = 0;

        for (Path selectedPath : selectedFiles) {
            if (existingPaths.add(selectedPath)) {
                ConversionFileRow row =
                        new ConversionFileRow(selectedPath);

                String uniqueOutputFileName =
                        outputFileNameService.createUnique(
                                row.getOutputFileName(),
                                existingOutputFileNames
                        );

                row.setOutputFileName(uniqueOutputFileName);
                filesTable.getItems().add(row);
                existingOutputFileNames.add(uniqueOutputFileName);

                addedFiles++;
            }
        }

        assignDefaultOutputDirectory(selectedFiles);
        updateSelectionCount();

        if (addedFiles == 0) {
            activityLogManager.add(
                    ActivityLogLevel.WARNING,
                    "Los archivos seleccionados ya estaban en la lista."
            );
            return;
        }

        String message = addedFiles == 1
                ? "Se agregó 1 archivo."
                : "Se agregaron " + addedFiles + " archivos.";

        activityLogManager.add(
                ActivityLogLevel.INFORMATION,
                message
        );
    }

    /**
     * Opens a directory chooser for selecting the XML output folder.
     */
    @FXML
    private void onChooseOutputDirectory() {
        fileDialogService.chooseOutputDirectory(
                getWindow(),
                getPreferredInitialLocation(null)
        ).ifPresent(selectedDirectory -> {
            outputDirectoryField.setText(
                    selectedDirectory.toString()
            );

            activityLogManager.add(
                    ActivityLogLevel.INFORMATION,
                    "Carpeta de salida seleccionada."
            );
        });
    }

    /**
     * Opens the configured output directory in the operating system file manager.
     */
    @FXML
    private void onOpenOutputDirectory() {
        Path outputDirectory = getOutputDirectory();

        if (outputDirectory == null) {
            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    "No se puede abrir la carpeta de salida porque "
                            + "la ubicación no existe o no es válida."
            );
            return;
        }

        try {
            desktopDirectoryOpener.open(outputDirectory);

            activityLogManager.add(
                    ActivityLogLevel.INFORMATION,
                    "Se abrió la carpeta de salida: "
                            + outputDirectory
            );
        } catch (IOException | SecurityException exception) {
            String failureMessage =
                    exception.getMessage() == null
                            ? "El sistema operativo rechazó la operación."
                            : exception.getMessage();

            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    "No se pudo abrir la carpeta de salida \""
                            + outputDirectory
                            + "\": "
                            + failureMessage
            );
        }
    }

    /**
     * Opens a file chooser and loads the selected SDMX DSD file.
     */
    @FXML
    private void onChooseDsdFile() {
        fileDialogService.chooseDsdFile(
                getWindow(),
                getPreferredInitialLocation(selectedDsdFile)
        ).ifPresent(this::loadDsdFile);
    }

    /**
     * Opens a file chooser for selecting the SDMX header properties file.
     */
    @FXML
    private void onChooseHeaderFile() {
        fileDialogService.chooseHeaderFile(
                getWindow(),
                getPreferredInitialLocation(selectedHeaderFile)
        ).ifPresent(this::loadHeaderFile);
    }

    /**
     * Removes every currently selected row from the conversion queue.
     */
    @FXML
    private void onRemoveSelectedFiles() {
        List<ConversionFileRow> selectedRows = List.copyOf(
                filesTable.getSelectionModel().getSelectedItems()
        );

        if (selectedRows.isEmpty()) {
            activityLogManager.add(
                    ActivityLogLevel.WARNING,
                    "No hay archivos seleccionados para eliminar."
            );
            return;
        }

        filesTable.getItems().removeAll(selectedRows);
        updateSelectionCount();

        activityLogManager.add(
                ActivityLogLevel.INFORMATION,
                "Se eliminaron " + selectedRows.size()
                        + " archivos de la lista."
        );
    }

    /**
     * Removes all input files from the conversion queue.
     */
    @FXML
    private void onClearFiles() {
        filesTable.getItems().clear();
        updateSelectionCount();

        activityLogManager.add(
                ActivityLogLevel.INFORMATION,
                "La lista de archivos está vacía."
        );
    }

    /**
     * Restores the conversion form and clears session diagnostics while
     * preserving the SDMX configuration.
     */
    @FXML
    private void onResetForm() {
        filesTable.getItems().clear();
        outputDirectoryField.clear();
        conversionDiagnosticManager.clear();
        updateSelectionCount();

        activityLogManager.reset(
                ActivityLogLevel.INFORMATION,
                "El formulario se restableció correctamente."
        );
    }

    /**
     * Shows or hides the activity log panel.
     */
    @FXML
    private void onToggleActivityLog() {
        activityLogManager.toggleVisibility();
    }

    /**
     * Removes every visual activity entry and stored conversion diagnostic.
     */
    @FXML
    private void onClearActivityLog() {
        conversionDiagnosticManager.clear();
        activityLogManager.clear();
    }

    /**
     * Copies the complete visual activity log to the system clipboard.
     */
    @FXML
    private void onCopyActivityLog() {
        activityLogManager.copyToClipboard();
    }

    /**
     * Exports the current activity history and stored conversion diagnostics to
     * a user-selected directory.
     */
    @FXML
    private void onSaveActivityLog() {
        List<ActivityLogEntry> entries =
                activityLogManager.getEntriesSnapshot();

        var diagnostics =
                conversionDiagnosticManager.snapshot();

        if (entries.isEmpty() && diagnostics.isEmpty()) {
            activityLogManager.add(
                    ActivityLogLevel.WARNING,
                    "No hay actividad para guardar."
            );
            return;
        }

        fileDialogService.chooseActivityLogDirectory(
                getWindow(),
                getPreferredInitialLocation(null)
        ).ifPresent(directory -> {
            try {
                Path logFile = activityLogFileExporter.export(
                        directory,
                        entries,
                        diagnostics
                );

                activityLogManager.add(
                        ActivityLogLevel.SUCCESS,
                        "El registro se guardó correctamente en \""
                                + logFile
                                + "\"."
                );
            } catch (IOException exception) {
                String failureMessage =
                        exception.getMessage() == null
                                ? "No se pudo escribir el archivo."
                                : exception.getMessage();

                activityLogManager.add(
                        ActivityLogLevel.ERROR,
                        "No se pudo guardar el registro en \""
                                + directory
                                + "\": "
                                + failureMessage
                );
            }
        });
    }

    /**
     * Opens the application settings window.
     */
    @FXML
    private void onOpenSettings() {
        try {
            settingsDialog.show(
                    getWindow(),
                    converterInstallationManager
            );
        } catch (IOException exception) {
            generalStatusLabel.setText(
                    "No fue posible abrir la configuración."
            );

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(getWindow());
            alert.setTitle("Error de configuración");
            alert.setHeaderText(
                    "No fue posible abrir la configuración de la aplicación."
            );
            alert.setContentText(
                    exception.getMessage() == null
                            ? "No se pudo cargar la ventana de configuración."
                            : exception.getMessage()
            );
            alert.showAndWait();
        }
    }

    /**
     * Validates the current configuration and starts the conversion batch.
     */
    @FXML
    private void onConvert() {
        if (conversionWorkflowManager.isRunning()) {
            return;
        }

        if (selectedDsdFile == null
                || selectedDsdMetadata == null
                || selectedHeaderFile == null) {
            activityLogManager.add(ActivityLogLevel.ERROR, "La configuración SDMX está incompleta.");
            return;
        }

        var installationValidation = converterInstallationManager.validateActiveInstallation();

        for (String warning : installationValidation.warnings()) {
            activityLogManager.add(ActivityLogLevel.WARNING, warning);
        }

        if (!installationValidation.isValid()) {
            for (String error : installationValidation.errors()) {
                activityLogManager.add(ActivityLogLevel.ERROR, error);
            }

            activityLogManager.add(ActivityLogLevel.ERROR,
                    "El Convertidor SDMX no está configurado "
                            + "correctamente."
            );
            return;
        }

        try {
            Path outputDirectory = Path.of(
                    outputDirectoryField.getText()
            ).toAbsolutePath().normalize();

            List<ConversionQueueItem> initialQueueItems =
                    conversionQueueFactory.create(
                            List.copyOf(filesTable.getItems()),
                            outputDirectory,
                            selectedDsdFile,
                            selectedHeaderFile,
                            selectedDsdMetadata,
                            conversionParameters
                    );

            var preparedQueue =
                    existingOutputCoordinator.prepare(
                            getWindow(),
                            initialQueueItems
                    );

            if (preparedQueue.isEmpty()) {
                filesTable.refresh();
                return;
            }

            List<ConversionQueueItem> queueItems =
                    preparedQueue.orElseThrow();

            filesTable.getItems().forEach(row -> {
                row.setStatus("Pendiente");
                row.clearConversionCounts();
            });

            conversionWorkflowManager.start(
                    conversionService,
                    converterInstallationManager
                            .getActiveInstallation(),
                    queueItems,
                    ignoredOutcomes -> filesTable.refresh(),
                    conversionDiagnosticManager::add
            );
        } catch (IllegalArgumentException exception) {
            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    exception.getMessage() == null
                            ? "La solicitud de conversión no es válida."
                            : exception.getMessage()
            );
        }
    }

    /**
     * Requests cancellation of the active conversion batch.
     */
    @FXML
    private void onCancelConversion() {
        conversionWorkflowManager.cancel();
    }

    private void configureTable() {
        fileNameColumn.setCellValueFactory(
                cell -> cell.getValue().fileNameProperty()
        );
        outputFileNameColumn.setCellValueFactory(
                cell -> cell.getValue().outputFileNameProperty()
        );
        filePathColumn.setCellValueFactory(
                cell -> cell.getValue().filePathProperty()
        );
        statusColumn.setCellValueFactory(
                cell -> cell.getValue().statusProperty()
        );
        statusColumn.setCellFactory(
                ignored -> new ConversionStatusTableCell()
        );
        seriesCountColumn.setCellValueFactory(
                cell -> cell.getValue().seriesCountProperty()
        );
        observationCountColumn.setCellValueFactory(
                cell -> cell.getValue().observationCountProperty()
        );

        seriesCountColumn.setStyle(
                "-fx-alignment: CENTER;"
        );
        observationCountColumn.setStyle(
                "-fx-alignment: CENTER;"
        );

        outputFileNameColumn.setCellFactory(
                TextFieldTableCell.forTableColumn()
        );
        outputFileNameColumn.setOnEditCommit(
                this::handleOutputFileNameEdit
        );

        filesTable.setEditable(true);
        filesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        filesTable.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );
    }

    private void handleOutputFileNameEdit(
            TableColumn.CellEditEvent<ConversionFileRow, String> event
    ) {
        ConversionFileRow editedRow = event.getRowValue();
        String previousFileName = editedRow.getOutputFileName();
        String enteredFileName = event.getNewValue();

        try {
            String normalizedFileName =
                    outputFileNameService.normalize(
                            enteredFileName
                    );

            outputFileNameService.validateUnique(
                    normalizedFileName,
                    getOutputFileNames(editedRow)
            );

            editedRow.setOutputFileName(normalizedFileName);
            editedRow.setStatus("Pendiente");
            editedRow.clearConversionCounts();

            activityLogManager.add(
                    ActivityLogLevel.INFORMATION,
                    "El archivo de salida para \""
                            + editedRow.getPath().getFileName()
                            + "\" se actualizó a \""
                            + normalizedFileName
                            + "\"."
            );
        } catch (IllegalArgumentException exception) {
            editedRow.setOutputFileName(previousFileName);

            String attemptedFileName =
                    enteredFileName == null || enteredFileName.isBlank()
                            ? "(nombre vacío)"
                            : enteredFileName;

            String failureMessage =
                    exception.getMessage() == null
                            ? "El nombre indicado no es válido."
                            : exception.getMessage();

            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    "No se pudo cambiar el archivo de salida de \""
                            + editedRow.getPath().getFileName()
                            + "\" a \""
                            + attemptedFileName
                            + "\": "
                            + failureMessage
            );
        } finally {
            Platform.runLater(filesTable::refresh);
        }
    }

    private void loadDsdFile(Path dsdFile) {
        try {
            DsdMetadata metadata =
                    dsdMetadataReader.read(dsdFile);

            selectedDsdFile = dsdFile;
            selectedDsdMetadata = metadata;

            dsdFileField.setText(dsdFile.toString());
            dsdAgencyValueLabel.setText(metadata.agencyId());
            dsdIdValueLabel.setText(metadata.id());
            dsdVersionValueLabel.setText(metadata.version());

            updateConfigurationSummary();

            activityLogManager.add(
                    ActivityLogLevel.SUCCESS,
                    "El DSD " + metadata.formattedIdentity()
                            + " se cargó correctamente."
            );
        } catch (IOException ignored) {
            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    "El archivo seleccionado no contiene "
                            + "una estructura DSD válida."
            );
        }
    }

    private void loadHeaderFile(Path headerFile) {
        if (!Files.isRegularFile(headerFile)
                || !Files.isReadable(headerFile)) {
            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    "No se puede leer el archivo de encabezado seleccionado."
            );
            return;
        }

        selectedHeaderFile = headerFile;
        headerFileField.setText(headerFile.toString());

        activityLogManager.add(
                ActivityLogLevel.SUCCESS,
                "El archivo de encabezado se cargó correctamente."
        );
    }

    private void assignDefaultOutputDirectory(
            List<Path> selectedFiles
    ) {
        if (!outputDirectoryField.getText().isBlank()
                || selectedFiles.isEmpty()) {
            return;
        }

        Path parent = selectedFiles.getFirst().getParent();

        if (parent != null) {
            outputDirectoryField.setText(parent.toString());
        }
    }

    private List<String> getOutputFileNames(
            ConversionFileRow ignoredRow
    ) {
        List<String> outputFileNames = new ArrayList<>();

        for (ConversionFileRow row : filesTable.getItems()) {
            if (row != ignoredRow) {
                outputFileNames.add(row.getOutputFileName());
            }
        }

        return outputFileNames;
    }

    private Path getPreferredInitialLocation(
            Path preferredLocation
    ) {
        if (preferredLocation != null) {
            return preferredLocation;
        }

        Path outputDirectory = getOutputDirectory();

        if (outputDirectory != null) {
            return outputDirectory;
        }

        if (!filesTable.getItems().isEmpty()) {
            return filesTable.getItems()
                    .getFirst()
                    .getPath();
        }

        return null;
    }

    private Path getOutputDirectory() {
        String value = outputDirectoryField.getText();

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            Path directory = Path.of(value)
                    .toAbsolutePath()
                    .normalize();

            return Files.isDirectory(directory)
                    ? directory
                    : null;
        } catch (InvalidPathException ignored) {
            return null;
        }
    }

    private void updateConfigurationSummary() {
        String summary = selectedDsdMetadata == null
                ? "Convertidor " + CONVERTER_VERSION
                + " · DSD sin configurar"
                : "Convertidor " + CONVERTER_VERSION
                + " · " + selectedDsdMetadata.formattedIdentity();

        configurationSummaryLabel.setText(summary);
    }

    private Window getWindow() {
        return filesTable.getScene().getWindow();
    }

    private void updateSelectionCount() {
        int size = filesTable.getItems().size();

        selectionCountLabel.setText(
                size == 1
                        ? "1 archivo seleccionado"
                        : size + " archivos seleccionados"
        );
    }
}