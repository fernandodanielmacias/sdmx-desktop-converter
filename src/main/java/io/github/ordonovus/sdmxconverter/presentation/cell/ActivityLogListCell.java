package io.github.ordonovus.sdmxconverter.presentation.cell;

import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogEntry;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogLevel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.List;

/**
 * Displays an activity log entry with its timestamp, level and message.
 */
public final class ActivityLogListCell extends ListCell<ActivityLogEntry> {

    private static final List<String> LEVEL_STYLE_CLASSES =
            Arrays.stream(ActivityLogLevel.values())
                    .map(ActivityLogLevel::getStyleClass)
                    .toList();

    private final HBox container = new HBox(12);
    private final Label timeLabel = new Label();
    private final Label levelLabel = new Label();
    private final Label messageLabel = new Label();

    /**
     * Creates and configures the visual elements used by the log cell.
     */
    public ActivityLogListCell() {
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("activity-log-row");

        timeLabel.setMinWidth(65);
        timeLabel.getStyleClass().add("activity-time");

        levelLabel.setMinWidth(95);
        levelLabel.getStyleClass().add("activity-level");

        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.getStyleClass().add("activity-message");

        HBox.setHgrow(messageLabel, javafx.scene.layout.Priority.ALWAYS);

        container.getChildren().addAll(
                timeLabel,
                levelLabel,
                messageLabel
        );
    }

    /**
     * Updates the cell according to the supplied activity entry.
     *
     * @param entry activity entry displayed by the cell
     * @param empty whether the cell is currently empty
     */
    @Override
    protected void updateItem(
            ActivityLogEntry entry,
            boolean empty
    ) {
        super.updateItem(entry, empty);

        if (empty || entry == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        timeLabel.setText(entry.formattedTime());
        levelLabel.setText(entry.level().getDisplayName());
        messageLabel.setText(entry.message());

        levelLabel.getStyleClass().removeAll(
                LEVEL_STYLE_CLASSES
        );
        levelLabel.getStyleClass().add(
                entry.level().getStyleClass()
        );

        setText(null);
        setGraphic(container);
    }
}
