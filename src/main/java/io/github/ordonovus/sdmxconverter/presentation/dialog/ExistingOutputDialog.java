package io.github.ordonovus.sdmxconverter.presentation.dialog;

import io.github.ordonovus.sdmxconverter.presentation.conversion.ExistingOutputDecision;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Requests a user decision when destination XML files already exist.
 */
public final class ExistingOutputDialog {

    private static final int MAXIMUM_DISPLAYED_FILES = 8;
    private static final double DIALOG_WIDTH = 660;
    private static final double FILE_NAME_WIDTH = 540;

    /**
     * Shows the existing-output confirmation dialog.
     *
     * <p>Closing the dialog without selecting an action is treated as a
     * cancellation. The cancellation button is configured as the safe default
     * action.</p>
     *
     * @param owner owner window of the dialog
     * @param existingOutputFiles existing destination XML files
     * @return action selected by the user
     */
    public ExistingOutputDecision show(
            Window owner,
            List<Path> existingOutputFiles
    ) {
        Objects.requireNonNull(owner, "owner");

        List<Path> safeFiles = List.copyOf(
                Objects.requireNonNull(
                        existingOutputFiles,
                        "existingOutputFiles"
                )
        );

        if (safeFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "existingOutputFiles must not be empty"
            );
        }

        ButtonType replaceButton = new ButtonType(
                "Sobrescribir",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType skipButton = new ButtonType(
                "Omitir existentes",
                ButtonBar.ButtonData.OTHER
        );

        ButtonType cancelButton = new ButtonType(
                "Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        Dialog<ExistingOutputDecision> dialog =
                new Dialog<>();

        dialog.initOwner(owner);
        dialog.setTitle("Archivos XML existentes");
        dialog.setResizable(true);

        dialog.getDialogPane()
                .getButtonTypes()
                .setAll(
                        skipButton,
                        replaceButton,
                        cancelButton
                );

        configureDialogPane(
                dialog,
                safeFiles
        );

        configureButtons(
                dialog,
                replaceButton,
                skipButton,
                cancelButton
        );

        dialog.setResultConverter(selectedButton -> {
            if (selectedButton == replaceButton) {
                return ExistingOutputDecision.REPLACE;
            }

            if (selectedButton == skipButton) {
                return ExistingOutputDecision.SKIP;
            }

            return ExistingOutputDecision.CANCEL;
        });

        return dialog.showAndWait()
                .orElse(ExistingOutputDecision.CANCEL);
    }

    private void configureDialogPane(
            Dialog<ExistingOutputDecision> dialog,
            List<Path> existingOutputFiles
    ) {
        String stylesheet = Objects.requireNonNull(
                ExistingOutputDialog.class.getResource(
                        "/css/application.css"
                ),
                "Application stylesheet was not found"
        ).toExternalForm();

        dialog.getDialogPane()
                .getStylesheets()
                .add(stylesheet);

        dialog.getDialogPane()
                .getStyleClass()
                .add("existing-output-dialog");

        dialog.getDialogPane().setPrefWidth(DIALOG_WIDTH);
        dialog.getDialogPane().setMinWidth(DIALOG_WIDTH);
        dialog.getDialogPane().setContent(
                createContent(existingOutputFiles)
        );
    }

    private VBox createContent(
            List<Path> existingOutputFiles
    ) {
        var content = new VBox(18);
        content.getStyleClass().add(
                "existing-output-content"
        );
        content.setPadding(
                new Insets(8, 10, 6, 10)
        );

        content.getChildren().addAll(
                createHeader(existingOutputFiles.size()),
                createFileSection(existingOutputFiles),
                createExplanationSection()
        );

        return content;
    }

    private HBox createHeader(
            int existingFileCount
    ) {
        var warningIcon = new Label("!");
        warningIcon.getStyleClass().add(
                "existing-output-warning-icon"
        );

        var title = new Label(
                existingFileCount == 1
                        ? "El archivo XML de salida ya existe"
                        : "Existen archivos XML de salida"
        );
        title.getStyleClass().add(
                "existing-output-title"
        );

        var description = new Label(
                existingFileCount == 1
                        ? "Selecciona qué debe hacer la aplicación "
                        + "con este archivo."
                        : "Selecciona qué debe hacer la aplicación con "
                        + existingFileCount
                        + " archivos."
        );
        description.setWrapText(true);
        description.getStyleClass().add(
                "existing-output-description"
        );

        var textContainer = new VBox(
                4,
                title,
                description
        );

        var header = new HBox(
                14,
                warningIcon,
                textContainer
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add(
                "existing-output-header"
        );

        return header;
    }

    private VBox createFileSection(
            List<Path> existingOutputFiles
    ) {
        var sectionTitle = new Label(
                "Archivos encontrados"
        );
        sectionTitle.getStyleClass().add(
                "existing-output-section-title"
        );

        var fileList = new VBox(6);
        fileList.getStyleClass().add(
                "existing-output-file-list"
        );

        existingOutputFiles.stream()
                .limit(MAXIMUM_DISPLAYED_FILES)
                .map(this::createFileLabel)
                .forEach(fileList.getChildren()::add);

        int hiddenFileCount =
                existingOutputFiles.size()
                        - MAXIMUM_DISPLAYED_FILES;

        if (hiddenFileCount > 0) {
            var remainingFilesLabel = new Label(
                    "Y " + hiddenFileCount
                            + " archivo(s) más."
            );
            remainingFilesLabel.getStyleClass().add(
                    "existing-output-more-files"
            );
            fileList.getChildren().add(
                    remainingFilesLabel
            );
        }

        return new VBox(
                8,
                sectionTitle,
                fileList
        );
    }

    private Label createFileLabel(
            Path outputFile
    ) {
        String fileName = outputFile
                .getFileName()
                .toString();

        var fileLabel = new Label(
                "XML  " + fileName
        );
        fileLabel.setMaxWidth(FILE_NAME_WIDTH);
        fileLabel.setTextOverrun(
                OverrunStyle.ELLIPSIS
        );
        fileLabel.setTooltip(
                new Tooltip(outputFile.toString())
        );
        fileLabel.getStyleClass().add(
                "existing-output-file"
        );

        return fileLabel;
    }

    private VBox createExplanationSection() {
        var safetyTitle = new Label(
                "Tus archivos actuales están protegidos"
        );
        safetyTitle.getStyleClass().add(
                "existing-output-safety-title"
        );

        TextFlow replaceExplanation = createOptionExplanation(
                "Sobrescribir: ",
                "reemplaza cada XML únicamente después de generar "
                        + "y validar correctamente su nueva versión."
        );

        TextFlow skipExplanation = createOptionExplanation(
                "Omitir existentes: ",
                "convierte solamente los archivos que aún no tienen "
                        + "un XML de salida."
        );

        var explanations = new VBox(
                10,
                replaceExplanation,
                skipExplanation
        );

        var section = new VBox(
                8,
                safetyTitle,
                explanations
        );
        section.getStyleClass().add(
                "existing-output-safety-box"
        );

        return section;
    }

    private TextFlow createOptionExplanation(
            String optionName,
            String description
    ) {
        var optionNameText = new Text(optionName);
        optionNameText.getStyleClass().add(
                "existing-output-option-name"
        );

        var descriptionText = new Text(description);
        descriptionText.getStyleClass().add(
                "existing-output-option-description"
        );

        var explanation = new TextFlow(
                optionNameText,
                descriptionText
        );
        explanation.setLineSpacing(2);

        return explanation;
    }

    private void configureButtons(
            Dialog<ExistingOutputDecision> dialog,
            ButtonType replaceButton,
            ButtonType skipButton,
            ButtonType cancelButton
    ) {
        Button replaceControl = (Button) dialog
                .getDialogPane()
                .lookupButton(replaceButton);

        Button skipControl = (Button) dialog
                .getDialogPane()
                .lookupButton(skipButton);

        Button cancelControl = (Button) dialog
                .getDialogPane()
                .lookupButton(cancelButton);

        replaceControl.getStyleClass().add(
                "existing-output-replace-button"
        );
        skipControl.getStyleClass().add(
                "existing-output-skip-button"
        );
        cancelControl.getStyleClass().add(
                "existing-output-cancel-button"
        );

        replaceControl.setDefaultButton(false);
        skipControl.setDefaultButton(false);
        cancelControl.setDefaultButton(true);
    }

}