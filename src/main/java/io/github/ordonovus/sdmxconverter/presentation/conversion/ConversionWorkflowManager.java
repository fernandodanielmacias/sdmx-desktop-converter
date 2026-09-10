package io.github.ordonovus.sdmxconverter.presentation.conversion;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionOutcome;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionResult;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionService;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.presentation.log.ActivityLogManager;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogLevel;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionFileRow;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionQueueItem;
import io.github.ordonovus.sdmxconverter.presentation.task.ConversionBatchTask;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Manages the JavaFX state associated with a batch conversion workflow.
 *
 * <p>This class coordinates the background task, progress controls,
 * table-row statuses and user-visible activity messages.</p>
 */
public final class ConversionWorkflowManager {

    private static final String STATUS_CONVERTING = "Convirtiendo";
    private static final String STATUS_COMPLETED = "Completado";
    private static final String STATUS_ERROR = "Error";
    private static final String STATUS_CANCELLED = "Cancelado";

    private final ActivityLogManager activityLogManager;
    private final Pane progressContainer;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label progressPercentageLabel;
    private final Button cancelButton;
    private final TableView<ConversionFileRow> filesTable;
    private final List<Node> guardedControls;

    private final BooleanProperty running =
            new SimpleBooleanProperty(false);

    private ConversionBatchTask activeTask;
    private int currentItemNumber;
    private int totalItems;

    /**
     * Creates a manager for the conversion workflow controls.
     *
     * @param activityLogManager activity log manager
     * @param progressContainer progress controls container
     * @param progressBar general batch progress bar
     * @param progressLabel current progress message label
     * @param progressPercentageLabel label displaying the completed percentage
     * @param cancelButton conversion cancellation button
     * @param filesTable conversion file table
     * @param guardedControls controls disabled while conversion is running
     */
    public ConversionWorkflowManager(
            ActivityLogManager activityLogManager,
            Pane progressContainer,
            ProgressBar progressBar,
            Label progressLabel,
            Label progressPercentageLabel,
            Button cancelButton,
            TableView<ConversionFileRow> filesTable,
            List<Node> guardedControls
    ) {
        this.activityLogManager = Objects.requireNonNull(
                activityLogManager,
                "activityLogManager"
        );

        this.progressContainer = Objects.requireNonNull(
                progressContainer,
                "progressContainer"
        );

        this.progressBar = Objects.requireNonNull(
                progressBar,
                "progressBar"
        );

        this.progressLabel = Objects.requireNonNull(
                progressLabel,
                "progressLabel"
        );

        this.progressPercentageLabel = Objects.requireNonNull(
                progressPercentageLabel,
                "progressPercentageLabel"
        );

        this.cancelButton = Objects.requireNonNull(
                cancelButton,
                "cancelButton"
        );

        this.filesTable = Objects.requireNonNull(
                filesTable,
                "filesTable"
        );

        this.guardedControls = List.copyOf(
                Objects.requireNonNull(
                        guardedControls,
                        "guardedControls"
                )
        );

        configureInitialState();
    }

    /**
     * Provides the read-only running state used by control bindings.
     *
     * @return observable running state
     */
    public ReadOnlyBooleanProperty runningProperty() {
        return running;
    }

    /**
     * Indicates whether a conversion batch is currently running.
     *
     * @return {@code true} while a batch task is active
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Starts a conversion batch.
     *
     * @param conversionService complete conversion service
     * @param installation active validated Converter installation
     * @param queueItems conversion queue
     * @param completionListener listener notified with completed outcomes
     */
    public void start(
            SdmxConversionService conversionService,
            ConverterInstallation installation,
            List<ConversionQueueItem> queueItems,
            Consumer<List<SdmxConversionOutcome>> completionListener
    ) {
        Objects.requireNonNull(
                conversionService,
                "conversionService"
        );
        Objects.requireNonNull(installation, "installation");
        Objects.requireNonNull(queueItems, "queueItems");
        Objects.requireNonNull(
                completionListener,
                "completionListener"
        );

        if (isRunning()) {
            throw new IllegalStateException(
                    "A conversion batch is already running"
            );
        }

        currentItemNumber = 0;
        totalItems = queueItems.size();

        ConversionBatchTask task =
                new ConversionBatchTask(
                        conversionService,
                        installation,
                        queueItems,
                        this::handleItemStarted,
                        this::handleItemFinished
                );

        activeTask = task;

        configureTaskBindings(task);
        setRunningState(true);

        activityLogManager.add(
                ActivityLogLevel.PROCESSING,
                totalItems == 1
                        ? "Se inició la conversión de 1 archivo."
                        : "Se inició la conversión de "
                        + totalItems
                        + " archivos."
        );

        task.setOnSucceeded(event -> {
            List<SdmxConversionOutcome> outcomes =
                    task.getValue();

            finishTask();

            long successfulCount = outcomes.stream()
                    .filter(SdmxConversionOutcome::isSuccessful)
                    .count();

            ActivityLogLevel level =
                    successfulCount == outcomes.size()
                            ? ActivityLogLevel.SUCCESS
                            : ActivityLogLevel.WARNING;

            activityLogManager.add(
                    level,
                    task.getMessage()
            );

            completionListener.accept(outcomes);
        });

        task.setOnFailed(event -> {
            Throwable failure = task.getException();

            markConvertingRowsAs(STATUS_ERROR);
            finishTask();

            activityLogManager.add(
                    ActivityLogLevel.ERROR,
                    "El proceso de conversión terminó inesperadamente: "
                            + requireFailureMessage(failure)
            );
        });

        task.setOnCancelled(event -> {
            markConvertingRowsAs(STATUS_CANCELLED);
            finishTask();

            activityLogManager.add(
                    ActivityLogLevel.WARNING,
                    "El proceso de conversión fue cancelado."
            );
        });

        Thread worker = new Thread(
                task,
                "sdmx-conversion-batch"
        );
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Requests cancellation of the active conversion batch.
     */
    public void cancel() {
        ConversionBatchTask task = activeTask;

        if (task == null || !isRunning()) {
            return;
        }

        cancelButton.setDisable(true);

        activityLogManager.add(
                ActivityLogLevel.WARNING,
                "Cancelando el proceso de conversión..."
        );

        task.cancel(true);
    }

    private void configureInitialState() {
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);

        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }

    private void configureTaskBindings(
            ConversionBatchTask task
    ) {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progressPercentageLabel.textProperty().unbind();

        progressBar.progressProperty().bind(
                task.progressProperty()
        );

        progressLabel.textProperty().bind(
                task.messageProperty()
        );

        progressPercentageLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> formatProgressPercentage(
                                task.getProgress()
                        ),
                        task.progressProperty()
                )
        );
    }

    private void setRunningState(boolean value) {
        running.set(value);

        progressContainer.setVisible(value);
        progressContainer.setManaged(value);

        cancelButton.setVisible(value);
        cancelButton.setManaged(value);
        cancelButton.setDisable(false);

        filesTable.setEditable(!value);

        for (Node control : guardedControls) {
            control.setDisable(value);
        }
    }

    private void finishTask() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progressPercentageLabel.textProperty().unbind();

        setRunningState(false);

        progressBar.setProgress(0);
        progressLabel.setText("");
        progressPercentageLabel.setText("0 %");

        currentItemNumber = 0;
        totalItems = 0;
        activeTask = null;
    }

    private void handleItemStarted(
            ConversionQueueItem queueItem
    ) {
        currentItemNumber++;

        queueItem.row().setStatus(STATUS_CONVERTING);
        queueItem.row().clearConversionCounts();

        String inputFileName = queueItem.request()
                .inputFile()
                .getFileName()
                .toString();

        activityLogManager.addToHistory(
                ActivityLogLevel.PROCESSING,
                "Convirtiendo: " + inputFileName
        );

        activityLogManager.showStatus(
                ActivityLogLevel.PROCESSING,
                "Conversión en curso: "
                        + currentItemNumber
                        + " de "
                        + totalItems
                        + "."
        );
    }

    private void handleItemFinished(
            ConversionQueueItem queueItem,
            SdmxConversionOutcome outcome
    ) {
        if (outcome.isSuccessful()) {
            handleSuccessfulItem(
                    queueItem,
                    outcome.result().orElseThrow()
            );
            return;
        }

        queueItem.row().setStatus(STATUS_ERROR);
        queueItem.row().clearConversionCounts();

        outcome.result().ifPresentOrElse(
                result -> logUnsuccessfulResult(
                        queueItem,
                        result
                ),
                () -> logExecutionFailure(
                        queueItem,
                        outcome.failure().orElseThrow()
                )
        );
    }

    private void handleSuccessfulItem(
            ConversionQueueItem queueItem,
            SdmxConversionResult result
    ) {
        queueItem.row().setStatus(STATUS_COMPLETED);
        queueItem.row().setConversionCounts(
                result.seriesCount(),
                result.observationCount()
        );

        activityLogManager.addToHistory(
                ActivityLogLevel.SUCCESS,
                queueItem.request()
                        .inputFile()
                        .getFileName()
                        + ": conversión completada. Series: "
                        + result.seriesCount()
                        + ", observaciones: "
                        + result.observationCount()
                        + "."
        );
    }

    private void logUnsuccessfulResult(
            ConversionQueueItem queueItem,
            SdmxConversionResult result
    ) {
        String fileName = queueItem.request()
                .inputFile()
                .getFileName()
                .toString();

        if (!result.executionResult().isSuccessful()) {
            activityLogManager.addToHistory(
                    ActivityLogLevel.ERROR,
                    fileName
                            + ": el Convertidor terminó con código "
                            + result.executionResult().exitCode()
                            + "."
            );
        }

        for (String error :
                result.xmlValidationResult().errors()) {
            activityLogManager.addToHistory(
                    ActivityLogLevel.ERROR,
                    fileName + ": " + error
            );
        }
    }

    private void logExecutionFailure(
            ConversionQueueItem queueItem,
            Throwable failure
    ) {
        activityLogManager.addToHistory(
                ActivityLogLevel.ERROR,
                queueItem.request()
                        .inputFile()
                        .getFileName()
                        + ": "
                        + requireFailureMessage(failure)
        );
    }

    private void markConvertingRowsAs(String status) {
        filesTable.getItems().stream()
                .filter(row -> STATUS_CONVERTING.equals(
                        row.statusProperty().get()
                ))
                .forEach(row -> row.setStatus(status));
    }

    private String formatProgressPercentage(
            double progress
    ) {
        if (progress < 0) {
            return "En curso";
        }

        long percentage = Math.round(progress * 100);

        return percentage + " %";
    }

    private String requireFailureMessage(
            Throwable failure
    ) {
        if (failure == null) {
            return "No se pudo determinar la causa.";
        }

        String message = failure.getMessage();

        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

}