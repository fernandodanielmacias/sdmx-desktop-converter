package io.github.ordonovus.sdmxconverter.presentation.cell;

import io.github.ordonovus.sdmxconverter.presentation.model.ConversionFileRow;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

import java.util.List;

/**
 * Displays a conversion status using a semantic visual indicator.
 */
public final class ConversionStatusTableCell
        extends TableCell<ConversionFileRow, String> {

    private static final List<String> STATUS_STYLE_CLASSES = List.of(
            "conversion-status-pending",
            "conversion-status-processing",
            "conversion-status-completed",
            "conversion-status-error",
            "conversion-status-cancelled",
            "conversion-status-skipped"
    );

    private final Label statusLabel = new Label();

    /**
     * Creates a centered conversion status cell.
     */
    public ConversionStatusTableCell() {
        setAlignment(Pos.CENTER);

        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.getStyleClass().add(
                "conversion-status-badge"
        );
    }

    /**
     * Updates the visual indicator according to the current row status.
     *
     * @param status current conversion status
     * @param empty whether the cell contains no row data
     */
    @Override
    protected void updateItem(
            String status,
            boolean empty
    ) {
        super.updateItem(status, empty);

        if (empty || status == null || status.isBlank()) {
            setText(null);
            setGraphic(null);
            return;
        }

        statusLabel.getStyleClass().removeAll(
                STATUS_STYLE_CLASSES
        );
        statusLabel.getStyleClass().add(
                getStatusStyleClass(status)
        );
        statusLabel.setText(status);

        setText(null);
        setGraphic(statusLabel);
    }

    private String getStatusStyleClass(String status) {
        return switch (status) {
            case "Convirtiendo" ->
                    "conversion-status-processing";
            case "Completado" ->
                    "conversion-status-completed";
            case "Error" ->
                    "conversion-status-error";
            case "Cancelado" ->
                    "conversion-status-cancelled";
            case "Omitido" ->
                    "conversion-status-skipped";
            default ->
                    "conversion-status-pending";
        };
    }
}