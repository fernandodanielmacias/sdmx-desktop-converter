package io.github.ordonovus.sdmxconverter.presentation.log;

import io.github.ordonovus.sdmxconverter.presentation.cell.ActivityLogListCell;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogEntry;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogLevel;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages the visual activity log and the general status message.
 */
public final class ActivityLogManager {

    private static final List<String> STATUS_STYLE_CLASSES = List.of(
            "status-information",
            "status-processing",
            "status-success",
            "status-warning",
            "status-error"
    );

    private final ListView<ActivityLogEntry> activityLogList;
    private final VBox activityLogPanel;
    private final Label generalStatusLabel;
    private final Button toggleActivityLogButton;
    private final ScrollPane contentScrollPane;

    /**
     * Creates and configures the activity log manager.
     *
     * @param activityLogList list displaying activity entries
     * @param activityLogPanel collapsible activity panel
     * @param generalStatusLabel label displaying the current general status
     * @param toggleActivityLogButton button controlling panel visibility
     * @param contentScrollPane main scrollable application content
     */
    public ActivityLogManager(
            ListView<ActivityLogEntry> activityLogList,
            VBox activityLogPanel,
            Label generalStatusLabel,
            Button toggleActivityLogButton,
            ScrollPane contentScrollPane
    ) {
        this.activityLogList = Objects.requireNonNull(
                activityLogList,
                "activityLogList"
        );
        this.activityLogPanel = Objects.requireNonNull(
                activityLogPanel,
                "activityLogPanel"
        );
        this.generalStatusLabel = Objects.requireNonNull(
                generalStatusLabel,
                "generalStatusLabel"
        );
        this.toggleActivityLogButton = Objects.requireNonNull(
                toggleActivityLogButton,
                "toggleActivityLogButton"
        );
        this.contentScrollPane = Objects.requireNonNull(
                contentScrollPane,
                "contentScrollPane"
        );

        configure();
    }

    /**
     * Adds an activity entry and updates the general status.
     *
     * @param level severity level assigned to the activity
     * @param message description displayed to the user
     */
    public void add(
            ActivityLogLevel level,
            String message
    ) {
        addToHistory(level, message);
        showStatus(level, message);
    }

    /**
     * Adds an entry to the activity history without changing the general
     * status message.
     *
     * @param level severity level assigned to the activity
     * @param message description stored in the activity history
     */
    public void addToHistory(
            ActivityLogLevel level,
            String message
    ) {
        ActivityLogEntry entry = ActivityLogEntry.now(
                requireLevel(level),
                requireMessage(message)
        );

        activityLogList.getItems().add(entry);
        Platform.runLater(this::scrollToLastEntry);
    }

    /**
     * Returns an immutable snapshot of the current activity history.
     *
     * @return activity entries available at the time of the request
     */
    public List<ActivityLogEntry> getEntriesSnapshot() {
        return List.copyOf(activityLogList.getItems());
    }

    /**
     * Updates the general status without adding an activity history entry.
     *
     * @param level severity level assigned to the status
     * @param message current general status message
     */
    public void showStatus(
            ActivityLogLevel level,
            String message
    ) {
        updateGeneralStatus(
                requireLevel(level),
                requireMessage(message)
        );
    }

    /**
     * Shows or hides the activity log panel.
     *
     * <p>When the panel is shown, the main content and activity list are moved
     * to their latest visible positions after JavaFX recalculates the layout.</p>
     */
    public void toggleVisibility() {
        boolean showPanel = !activityLogPanel.isVisible();

        setActivityLogVisible(showPanel);

        if (showPanel) {
            Platform.runLater(
                    this::scrollToVisibleActivityPanel
            );
        }
    }

    /**
     * Shows the activity log and moves it into the visible content area.
     *
     * <p>If the panel is already visible, it remains open and is only moved to
     * its latest visible position.</p>
     */
    public void showActivityLog() {
        setActivityLogVisible(true);

        Platform.runLater(
                this::scrollToVisibleActivityPanel
        );
    }

    /**
     * Removes all activity entries and updates the general status.
     */
    public void clear() {
        activityLogList.getItems().clear();

        showStatus(
                ActivityLogLevel.INFORMATION,
                "El registro de actividad está vacío."
        );
    }

    /**
     * Clears the activity history and adds a new initial entry.
     *
     * @param level severity level assigned to the new entry
     * @param message description of the reset operation
     */
    public void reset(
            ActivityLogLevel level,
            String message
    ) {
        activityLogList.getItems().clear();
        add(level, message);
    }

    /**
     * Copies the complete activity history to the system clipboard.
     */
    public void copyToClipboard() {
        if (activityLogList.getItems().isEmpty()) {
            showStatus(
                    ActivityLogLevel.WARNING,
                    "No hay actividad para copiar."
            );
            return;
        }

        String logText = activityLogList.getItems()
                .stream()
                .map(this::formatEntry)
                .collect(
                        Collectors.joining(
                                System.lineSeparator()
                        )
                );

        var clipboardContent = new ClipboardContent();
        clipboardContent.putString(logText);

        Clipboard.getSystemClipboard().setContent(
                clipboardContent
        );

        showStatus(
                ActivityLogLevel.SUCCESS,
                "El registro de actividad se copió al portapapeles."
        );
    }

    private void setActivityLogVisible(
            boolean visible
    ) {
        activityLogPanel.setVisible(visible);

        toggleActivityLogButton.setText(
                visible
                        ? "Ocultar actividad"
                        : "Mostrar actividad"
        );
    }

    private void configure() {
        activityLogList.setCellFactory(
                ignored -> new ActivityLogListCell()
        );

        activityLogPanel.managedProperty().bind(
                activityLogPanel.visibleProperty()
        );
        activityLogPanel.setVisible(false);
    }

    private void updateGeneralStatus(
            ActivityLogLevel level,
            String message
    ) {
        generalStatusLabel.getStyleClass().removeAll(
                STATUS_STYLE_CLASSES
        );
        generalStatusLabel.getStyleClass().add(
                getStatusStyleClass(level)
        );

        String displayedMessage = switch (level) {
            case WARNING -> "Advertencia: " + message;
            case ERROR -> "Error: " + message;
            default -> message;
        };

        generalStatusLabel.setText(displayedMessage);
    }

    private ActivityLogLevel requireLevel(
            ActivityLogLevel level
    ) {
        return Objects.requireNonNull(level, "level");
    }

    private String requireMessage(String message) {
        String validatedMessage = Objects.requireNonNull(
                message,
                "message"
        );

        if (validatedMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank"
            );
        }

        return validatedMessage;
    }

    private String getStatusStyleClass(
            ActivityLogLevel level
    ) {
        return switch (level) {
            case INFORMATION -> "status-information";
            case PROCESSING -> "status-processing";
            case SUCCESS -> "status-success";
            case WARNING -> "status-warning";
            case ERROR -> "status-error";
        };
    }

    private String formatEntry(ActivityLogEntry entry) {
        return "%s  %-11s  %s".formatted(
                entry.formattedTime(),
                entry.level().getDisplayName(),
                entry.message()
        );
    }

    private void scrollToVisibleActivityPanel() {
        activityLogPanel.applyCss();
        activityLogPanel.layout();

        contentScrollPane.applyCss();
        contentScrollPane.layout();
        contentScrollPane.setVvalue(
                contentScrollPane.getVmax()
        );

        scrollToLastEntry();

        Platform.runLater(() -> {
            contentScrollPane.setVvalue(
                    contentScrollPane.getVmax()
            );
            scrollToLastEntry();
        });
    }

    private void scrollToLastEntry() {
        int lastIndex = activityLogList.getItems().size() - 1;

        if (lastIndex >= 0) {
            activityLogList.scrollTo(lastIndex);
        }
    }

}