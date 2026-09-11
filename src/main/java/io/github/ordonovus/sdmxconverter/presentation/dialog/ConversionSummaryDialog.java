package io.github.ordonovus.sdmxconverter.presentation.dialog;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionOutcome;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.presentation.conversion.ConversionBatchDiagnostic;
import io.github.ordonovus.sdmxconverter.presentation.conversion.ConversionBatchStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Displays the final summary of a conversion batch.
 */
public final class ConversionSummaryDialog {

    private static final int MAXIMUM_DISPLAYED_ISSUES = 8;
    private static final double DIALOG_WIDTH = 700;

    /**
     * Shows the conversion summary dialog.
     *
     * @param owner owner window of the dialog
     * @param diagnostic completed batch diagnostic
     * @return {@code true} when the user requests to view activity details
     */
    public boolean show(
            Window owner,
            ConversionBatchDiagnostic diagnostic
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(diagnostic, "diagnostic");

        ButtonType activityButton = new ButtonType(
                "Mostrar actividad",
                ButtonBar.ButtonData.OTHER
        );

        ButtonType closeButton = new ButtonType(
                "Cerrar",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        Dialog<Boolean> dialog = new Dialog<>();

        dialog.initOwner(owner);
        dialog.setTitle("Resultado de la conversión");
        dialog.setResizable(true);

        dialog.getDialogPane()
                .getButtonTypes()
                .setAll(
                        activityButton,
                        closeButton
                );

        configureDialogPane(
                dialog,
                diagnostic
        );

        configureButtons(
                dialog,
                activityButton,
                closeButton
        );

        dialog.setResultConverter(
                selectedButton ->
                        selectedButton == activityButton
        );

        return dialog.showAndWait()
                .orElse(false);
    }

    private void configureDialogPane(
            Dialog<Boolean> dialog,
            ConversionBatchDiagnostic diagnostic
    ) {
        String stylesheet = Objects.requireNonNull(
                ConversionSummaryDialog.class.getResource(
                        "/css/application.css"
                ),
                "Application stylesheet was not found"
        ).toExternalForm();

        dialog.getDialogPane()
                .getStylesheets()
                .add(stylesheet);

        dialog.getDialogPane()
                .getStyleClass()
                .add("conversion-summary-dialog");

        dialog.getDialogPane().setPrefWidth(DIALOG_WIDTH);
        dialog.getDialogPane().setMinWidth(DIALOG_WIDTH);
        dialog.getDialogPane().setContent(
                createContent(diagnostic)
        );
    }

    private VBox createContent(
            ConversionBatchDiagnostic diagnostic
    ) {
        var content = new VBox(18);
        content.setPadding(
                new Insets(8, 10, 6, 10)
        );
        content.getStyleClass().add(
                "conversion-summary-content"
        );

        content.getChildren().add(
                createHeader(diagnostic)
        );
        content.getChildren().add(
                createMetrics(diagnostic)
        );

        VBox issuesSection =
                createIssuesSection(diagnostic);

        if (issuesSection != null) {
            content.getChildren().add(issuesSection);
        }

        return content;
    }

    private HBox createHeader(
            ConversionBatchDiagnostic diagnostic
    ) {
        var icon = new Label(
                getHeaderIcon(diagnostic)
        );
        icon.getStyleClass().addAll(
                "conversion-summary-icon",
                getHeaderStyleClass(diagnostic)
        );

        var title = new Label(
                getHeaderTitle(diagnostic)
        );
        title.getStyleClass().add(
                "conversion-summary-title"
        );

        var description = new Label(
                getHeaderDescription(diagnostic)
        );
        description.setWrapText(true);
        description.getStyleClass().add(
                "conversion-summary-description"
        );

        var textContainer = new VBox(
                4,
                title,
                description
        );

        var header = new HBox(
                14,
                icon,
                textContainer
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll(
                "conversion-summary-header",
                getHeaderStyleClass(diagnostic)
        );

        return header;
    }

    private GridPane createMetrics(
            ConversionBatchDiagnostic diagnostic
    ) {
        long seriesCount = diagnostic.outcomes()
                .stream()
                .filter(SdmxConversionOutcome::isSuccessful)
                .map(SdmxConversionOutcome::result)
                .mapToLong(result -> result.orElseThrow().seriesCount())
                .sum();

        long observationCount = diagnostic.outcomes()
                .stream()
                .filter(SdmxConversionOutcome::isSuccessful)
                .map(SdmxConversionOutcome::result)
                .mapToLong(
                        result -> result.orElseThrow()
                                .observationCount()
                )
                .sum();

        var metrics = new GridPane();
        metrics.setHgap(10);
        metrics.setVgap(10);
        metrics.getStyleClass().add(
                "conversion-summary-metrics"
        );

        metrics.add(
                createMetric(
                        Long.toString(diagnostic.successfulCount()),
                        "Correctos",
                        "conversion-summary-metric-success"
                ),
                0,
                0
        );

        metrics.add(
                createMetric(
                        Long.toString(diagnostic.failedCount()),
                        "Con error",
                        "conversion-summary-metric-error"
                ),
                1,
                0
        );

        metrics.add(
                createMetric(
                        Integer.toString(diagnostic.unprocessedCount()),
                        "Sin procesar",
                        "conversion-summary-metric-pending"
                ),
                2,
                0
        );

        metrics.add(
                createMetric(
                        Long.toString(seriesCount),
                        "Series",
                        "conversion-summary-metric-information"
                ),
                0,
                1
        );

        metrics.add(
                createMetric(
                        Long.toString(observationCount),
                        "Observaciones",
                        "conversion-summary-metric-information"
                ),
                1,
                1,
                2,
                1
        );

        return metrics;
    }

    private VBox createMetric(
            String value,
            String description,
            String styleClass
    ) {
        var valueLabel = new Label(value);
        valueLabel.getStyleClass().add(
                "conversion-summary-metric-value"
        );

        var descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add(
                "conversion-summary-metric-label"
        );

        var metric = new VBox(
                2,
                valueLabel,
                descriptionLabel
        );
        metric.setAlignment(Pos.CENTER);
        metric.setMaxWidth(Double.MAX_VALUE);
        metric.getStyleClass().addAll(
                "conversion-summary-metric",
                styleClass
        );

        GridPane.setFillWidth(metric, true);

        return metric;
    }

    private VBox createIssuesSection(
            ConversionBatchDiagnostic diagnostic
    ) {
        List<String> issues = createIssueDescriptions(
                diagnostic
        );

        if (issues.isEmpty()) {
            return null;
        }

        var title = new Label(
                "Archivos que requieren atención"
        );
        title.getStyleClass().add(
                "conversion-summary-issues-title"
        );

        String issueText = issues.stream()
                .limit(MAXIMUM_DISPLAYED_ISSUES)
                .collect(
                        Collectors.joining(
                                System.lineSeparator()
                        )
                );

        int hiddenIssueCount =
                issues.size() - MAXIMUM_DISPLAYED_ISSUES;

        if (hiddenIssueCount > 0) {
            issueText += System.lineSeparator()
                    + "• Y "
                    + hiddenIssueCount
                    + " archivo(s) más.";
        }

        var issueLabel = new Label(issueText);
        issueLabel.setWrapText(true);
        issueLabel.getStyleClass().add(
                "conversion-summary-issues"
        );

        return new VBox(
                8,
                title,
                issueLabel
        );
    }

    private List<String> createIssueDescriptions(
            ConversionBatchDiagnostic diagnostic
    ) {
        List<String> failedIssues =
                diagnostic.outcomes()
                        .stream()
                        .filter(outcome -> !outcome.isSuccessful())
                        .map(outcome -> "• "
                                + getFileName(outcome.request())
                                + " — Error")
                        .toList();

        Set<SdmxConversionRequest> processedRequests =
                diagnostic.outcomes()
                        .stream()
                        .map(SdmxConversionOutcome::request)
                        .collect(Collectors.toSet());

        List<String> unprocessedIssues =
                diagnostic.requests()
                        .stream()
                        .filter(request ->
                                !processedRequests.contains(request))
                        .map(request -> "• "
                                + getFileName(request)
                                + " — Sin procesar")
                        .toList();

        return java.util.stream.Stream.concat(
                failedIssues.stream(),
                unprocessedIssues.stream()
        ).toList();
    }

    private String getFileName(
            SdmxConversionRequest request
    ) {
        return request.inputFile()
                .getFileName()
                .toString();
    }

    private String getHeaderIcon(
            ConversionBatchDiagnostic diagnostic
    ) {
        if (diagnostic.status()
                == ConversionBatchStatus.CANCELLED) {
            return "!";
        }

        if (diagnostic.status()
                == ConversionBatchStatus.FAILED
                || diagnostic.failedCount() > 0) {
            return "!";
        }

        return "✓";
    }

    private String getHeaderTitle(
            ConversionBatchDiagnostic diagnostic
    ) {
        if (diagnostic.status()
                == ConversionBatchStatus.CANCELLED) {
            return "Conversión cancelada";
        }

        if (diagnostic.status()
                == ConversionBatchStatus.FAILED) {
            return "La conversión se interrumpió";
        }

        if (diagnostic.failedCount() > 0) {
            return "Conversión finalizada con incidencias";
        }

        return diagnostic.successfulCount() == 1
                ? "Conversión completada"
                : "Conversiones completadas";
    }

    private String getHeaderDescription(
            ConversionBatchDiagnostic diagnostic
    ) {
        if (diagnostic.status()
                == ConversionBatchStatus.CANCELLED) {
            return "Los archivos completados se conservaron y los "
                    + "elementos restantes no fueron procesados.";
        }

        if (diagnostic.status()
                == ConversionBatchStatus.FAILED) {
            return "Un error inesperado impidió continuar el proceso. "
                    + "Consulta el registro de actividad.";
        }

        if (diagnostic.failedCount() > 0) {
            return "El proceso terminó, pero algunos archivos requieren "
                    + "revisión.";
        }

        return "Todos los archivos procesados generaron un XML válido.";
    }

    private String getHeaderStyleClass(
            ConversionBatchDiagnostic diagnostic
    ) {
        if (diagnostic.status()
                == ConversionBatchStatus.CANCELLED) {
            return "conversion-summary-warning";
        }

        if (diagnostic.status()
                == ConversionBatchStatus.FAILED
                || diagnostic.failedCount() > 0) {
            return "conversion-summary-error";
        }

        return "conversion-summary-success";
    }

    private void configureButtons(
            Dialog<Boolean> dialog,
            ButtonType activityButton,
            ButtonType closeButton
    ) {
        Button activityControl = (Button) dialog
                .getDialogPane()
                .lookupButton(activityButton);

        Button closeControl = (Button) dialog
                .getDialogPane()
                .lookupButton(closeButton);

        activityControl.getStyleClass().add(
                "conversion-summary-activity-button"
        );
        closeControl.getStyleClass().add(
                "conversion-summary-close-button"
        );

        activityControl.setDefaultButton(false);
        closeControl.setDefaultButton(true);
    }

}