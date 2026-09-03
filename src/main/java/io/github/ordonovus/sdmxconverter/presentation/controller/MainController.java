package io.github.ordonovus.sdmxconverter.presentation.controller;

import io.github.ordonovus.sdmxconverter.presentation.cell.ActivityLogListCell;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogEntry;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogLevel;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionFileRow;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controls the main conversion screen and its file-selection actions.
 */
public final class MainController {

    private static final Pattern INVALID_WINDOWS_FILE_CHARACTERS =
            Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");

    private static final Pattern RESERVED_WINDOWS_FILE_NAME =
            Pattern.compile(
                    "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final List<String> STATUS_STYLE_CLASSES = List.of(
            "status-information",
            "status-processing",
            "status-success",
            "status-warning",
            "status-error"
    );

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
    private TextField outputDirectoryField;

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

    /**
     * Configures table columns, selection behavior and control bindings.
     */
    @FXML
    private void initialize() {
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

        configureEditableOutputFileNameColumn();
        configureActivityLog();

        filesTable.setEditable(true);
        filesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        filesTable.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );

        convertButton.disableProperty().bind(
                Bindings.isEmpty(filesTable.getItems())
                        .or(outputDirectoryField.textProperty().isEmpty())
        );

        updateSelectionCount();
        addActivity(ActivityLogLevel.INFORMATION, "Aplicación lista para seleccionar archivos.");
    }

    /**
     * Opens a multiple-selection dialog for Excel input files.
     */
    @FXML
    private void onChooseFiles() {
        var chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivos Excel");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Archivos Excel (*.xls, *.xlsx)",
                        "*.xls",
                        "*.xlsx"
                )
        );

        List<File> selectedFiles =
                chooser.showOpenMultipleDialog(getWindow());

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        Set<Path> existingPaths = new HashSet<>();

        for (ConversionFileRow row : filesTable.getItems()) {
            existingPaths.add(row.getPath());
        }

        int addedFiles = 0;

        for (File file : selectedFiles) {
            Path selectedPath = file.toPath()
                    .toAbsolutePath()
                    .normalize();

            if (existingPaths.add(selectedPath)) {
                ConversionFileRow row =
                        new ConversionFileRow(selectedPath);

                row.setOutputFileName(
                        createUniqueOutputFileName(
                                row.getOutputFileName()
                        )
                );

                filesTable.getItems().add(row);
                addedFiles++;
            }
        }

        if (outputDirectoryField.getText().isBlank()) {
            Path firstFile = selectedFiles.getFirst()
                    .toPath()
                    .toAbsolutePath()
                    .normalize();

            Path parent = firstFile.getParent();

            if (parent != null) {
                outputDirectoryField.setText(parent.toString());
            }
        }

        updateSelectionCount();

        if (addedFiles == 0) {
            addActivity(ActivityLogLevel.WARNING, "Los archivos seleccionados ya estaban en la lista.");
        } else {
            String message = addedFiles == 1
                    ? "Se agregó 1 archivo."
                    : "Se agregaron " + addedFiles + " archivos.";

            addActivity(ActivityLogLevel.INFORMATION, message);
        }
    }

    /**
     * Opens a directory chooser for selecting the XML output folder.
     */
    @FXML
    private void onChooseOutputDirectory() {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta de salida");

        File currentDirectory = getCurrentOutputDirectory();

        if (currentDirectory != null) {
            chooser.setInitialDirectory(currentDirectory);
        }

        File selectedDirectory = chooser.showDialog(getWindow());

        if (selectedDirectory != null) {
            outputDirectoryField.setText(selectedDirectory.getAbsolutePath());
            generalStatusLabel.setText("Carpeta de salida seleccionada.");
        }
    }

    /**
     * Removes every currently selected row from the conversion queue.
     */
    @FXML
    private void onRemoveSelectedFiles() {
        List<ConversionFileRow> selectedRows = List.copyOf(
                filesTable.getSelectionModel().getSelectedItems()
        );

        filesTable.getItems().removeAll(selectedRows);
        updateSelectionCount();

        if (selectedRows.isEmpty()) {
            addActivity(ActivityLogLevel.WARNING, "No hay archivos seleccionados para eliminar.");
        } else {
            addActivity(ActivityLogLevel.INFORMATION, "Se eliminaron " + selectedRows.size() + " archivos de la lista.");
        }
    }

    /**
     * Removes all input files from the conversion queue.
     */
    @FXML
    private void onClearFiles() {
        filesTable.getItems().clear();
        updateSelectionCount();

        addActivity(ActivityLogLevel.INFORMATION, "La lista de archivos está vacía.");
    }

    /**
     * Restores the conversion form to its initial state.
     */
    @FXML
    private void onResetForm() {
        filesTable.getItems().clear();
        outputDirectoryField.clear();
        activityLogList.getItems().clear();

        updateSelectionCount();

        addActivity(ActivityLogLevel.INFORMATION, "El formulario se restableció correctamente.");
    }

    /**
     * Shows or hides the activity log panel.
     */
    @FXML
    private void onToggleActivityLog() {
        boolean showPanel = !activityLogPanel.isVisible();

        activityLogPanel.setVisible(showPanel);

        toggleActivityLogButton.setText(showPanel ? "Ocultar actividad" : "Mostrar actividad");

        if (showPanel) {
            Platform.runLater(() -> {
                contentScrollPane.setVvalue(1.0);

                if (!activityLogList.getItems().isEmpty()) {
                    activityLogList.scrollTo(activityLogList.getItems().size() - 1);
                }
            });
        }
    }

    /**
     * Removes every entry from the visual activity log.
     */
    @FXML
    private void onClearActivityLog() {
        activityLogList.getItems().clear();

        updateGeneralStatus(ActivityLogLevel.INFORMATION, "El registro de actividad está vacío.");
    }

    /**
     * Copies the complete visual activity log to the system clipboard.
     */
    @FXML
    private void onCopyActivityLog() {
        if (activityLogList.getItems().isEmpty()) {
            updateGeneralStatus(ActivityLogLevel.WARNING, "No hay actividad para copiar.");
            return;
        }

        String logText = activityLogList.getItems()
                .stream()
                .map(this::formatActivityLogEntry)
                .collect(Collectors.joining(System.lineSeparator())
                );

        var clipboardContent = new ClipboardContent();
        clipboardContent.putString(logText);

        Clipboard.getSystemClipboard().setContent(clipboardContent);

        updateGeneralStatus(ActivityLogLevel.SUCCESS, "El registro de actividad se copió al portapapeles.");
    }

    /**
     * Handles the conversion action until the Converter integration is added.
     */
    @FXML
    private void onConvert() {
        addActivity(ActivityLogLevel.WARNING, "La integración con el Convertidor SDMX todavía no está disponible." );
    }

    private void configureEditableOutputFileNameColumn() {
        outputFileNameColumn.setCellFactory(
                TextFieldTableCell.forTableColumn()
        );

        outputFileNameColumn.setOnEditCommit(event -> {
            ConversionFileRow row = event.getRowValue();

            try {
                String normalizedFileName =
                        normalizeOutputFileName(event.getNewValue());

                validateUniqueOutputFileName(row, normalizedFileName);

                row.setOutputFileName(normalizedFileName);
                row.setStatus("Pendiente");

                addActivity(ActivityLogLevel.INFORMATION, "Nombre del archivo de salida actualizado.");
            } catch (IllegalArgumentException exception) {
                addActivity(ActivityLogLevel.ERROR, exception.getMessage());
            } finally {
                Platform.runLater(filesTable::refresh);
            }
        });
    }

    private void configureActivityLog() {
        activityLogList.setCellFactory(ignored -> new ActivityLogListCell());

        activityLogPanel.managedProperty().bind(activityLogPanel.visibleProperty());
        activityLogPanel.setVisible(false);
    }

    private void addActivity(
            ActivityLogLevel level,
            String message
    ) {
        ActivityLogEntry entry = ActivityLogEntry.now(level, message);

        activityLogList.getItems().add(entry);
        updateGeneralStatus(level, message);

        Platform.runLater(() ->
                activityLogList.scrollTo(activityLogList.getItems().size() - 1)
        );
    }

    private void updateGeneralStatus(
            ActivityLogLevel level,
            String message
    ) {
        generalStatusLabel.getStyleClass().removeAll(STATUS_STYLE_CLASSES);
        generalStatusLabel.getStyleClass().add(getStatusStyleClass(level));

        String displayedMessage = switch (level) {
            case WARNING -> "Advertencia: " + message;
            case ERROR -> "Error: " + message;
            default -> message;
        };

        generalStatusLabel.setText(displayedMessage);
    }

    private String getStatusStyleClass(ActivityLogLevel level) {
        return switch (level) {
            case INFORMATION -> "status-information";
            case PROCESSING -> "status-processing";
            case SUCCESS -> "status-success";
            case WARNING -> "status-warning";
            case ERROR -> "status-error";
        };
    }

    private String formatActivityLogEntry(ActivityLogEntry entry) {
        return "%s  %-11s  %s".formatted(
                entry.formattedTime(),
                entry.level().getDisplayName(),
                entry.message()
        );
    }

    private String normalizeOutputFileName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo de salida no puede estar vacío.");
        }

        String normalizedValue = value.trim();

        if (INVALID_WINDOWS_FILE_CHARACTERS
                .matcher(normalizedValue)
                .find()) {
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos por Windows.");
        }

        if (normalizedValue.endsWith(".")) {
            throw new IllegalArgumentException("El nombre del archivo no puede terminar con un punto.");
        }

        if (!normalizedValue.toLowerCase(Locale.ROOT)
                .endsWith(".xml")) {
            normalizedValue += ".xml";
        }

        String baseName = normalizedValue.substring(
                0,
                normalizedValue.length() - ".xml".length()
        );

        if (baseName.isBlank()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre antes de la extensión.");
        }

        if (RESERVED_WINDOWS_FILE_NAME
                .matcher(baseName)
                .matches()) {
            throw new IllegalArgumentException("El nombre está reservado por Windows.");
        }

        return normalizedValue;
    }


    private void validateUniqueOutputFileName(
            ConversionFileRow editedRow,
            String outputFileName
    ) {
        if (outputFileNameExists(outputFileName, editedRow)) {
            throw new IllegalArgumentException("Ya existe otro archivo de salida con ese nombre.");
        }
    }

    private String createUniqueOutputFileName(
            String preferredFileName
    ) {
        if (!outputFileNameExists(preferredFileName, null)) {
            return preferredFileName;
        }

        int extensionIndex = preferredFileName.lastIndexOf('.');

        String baseName = extensionIndex > 0
                ? preferredFileName.substring(0, extensionIndex)
                : preferredFileName;

        String extension = extensionIndex > 0 ? preferredFileName.substring(extensionIndex) : "";

        int suffix = 2;
        String candidate;

        do {
            candidate = "%s (%d)%s".formatted(
                    baseName,
                    suffix,
                    extension
            );
            suffix++;
        } while (outputFileNameExists(candidate, null));

        return candidate;
    }

    private boolean outputFileNameExists(
            String outputFileName,
            ConversionFileRow ignoredRow
    ) {
        return filesTable.getItems()
                .stream()
                .filter(row -> row != ignoredRow)
                .map(ConversionFileRow::getOutputFileName)
                .anyMatch(existingName ->
                        existingName.equalsIgnoreCase(outputFileName)
                );
    }

    private File getCurrentOutputDirectory() {
        String value = outputDirectoryField.getText();

        if (value == null || value.isBlank()) {
            return null;
        }

        File directory = new File(value);

        return directory.isDirectory()
                ? directory
                : null;
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
