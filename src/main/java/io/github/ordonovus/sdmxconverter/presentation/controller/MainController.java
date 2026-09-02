package io.github.ordonovus.sdmxconverter.presentation.controller;

import io.github.ordonovus.sdmxconverter.presentation.model.ConversionFileRow;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controls the main conversion screen and its file-selection actions.
 */
public final class MainController {

    @FXML
    private TableView<ConversionFileRow> filesTable;

    @FXML
    private TableColumn<ConversionFileRow, String> fileNameColumn;

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

    /**
     * Configures table columns, selection behavior and control bindings.
     */
    @FXML
    private void initialize() {
        fileNameColumn.setCellValueFactory(
                cell -> cell.getValue().fileNameProperty()
        );
        filePathColumn.setCellValueFactory(
                cell -> cell.getValue().filePathProperty()
        );
        statusColumn.setCellValueFactory(
                cell -> cell.getValue().statusProperty()
        );

        filesTable.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );

        convertButton.disableProperty().bind(
                Bindings.isEmpty(filesTable.getItems())
                        .or(outputDirectoryField.textProperty().isEmpty())
        );

        updateSelectionCount();
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
                filesTable.getItems().add(
                        new ConversionFileRow(selectedPath)
                );
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
        generalStatusLabel.setText(
                addedFiles == 1
                        ? "Se agregó 1 archivo."
                        : "Se agregaron " + addedFiles + " archivos."
        );
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
            outputDirectoryField.setText(
                    selectedDirectory.getAbsolutePath()
            );
            generalStatusLabel.setText(
                    "Carpeta de salida seleccionada."
            );
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

        generalStatusLabel.setText(
                selectedRows.isEmpty()
                        ? "No hay archivos seleccionados para eliminar."
                        : "Se eliminaron " + selectedRows.size()
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
        generalStatusLabel.setText("La lista de archivos está vacía.");
    }

    /**
     * Handles the conversion action until the Converter integration is added.
     */
    @FXML
    private void onConvert() {
        generalStatusLabel.setText(
                "La integración con SDMX Converter se agregará "
                        + "en el siguiente módulo."
        );
    }

    private File getCurrentOutputDirectory() {
        String value = outputDirectoryField.getText();

        if (value == null || value.isBlank()) {
            return null;
        }

        File directory = new File(value);
        return directory.isDirectory() ? directory : null;
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
