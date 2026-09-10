package io.github.ordonovus.sdmxconverter.presentation.task;

import io.github.ordonovus.sdmxconverter.application.conversion.InvalidConversionRequestException;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionOutcome;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionResult;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionService;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionQueueItem;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Executes a collection of SDMX conversion queue items sequentially without
 * blocking the JavaFX Application Thread.
 *
 * <p>Individual conversion failures do not prevent the remaining queue
 * items from being processed. Outcomes completed before cancellation or an
 * unexpected task failure remain available for diagnostic purposes.</p>
 */
public final class ConversionBatchTask
        extends Task<List<SdmxConversionOutcome>> {

    private final SdmxConversionService conversionService;
    private final ConverterInstallation installation;
    private final List<ConversionQueueItem> queueItems;
    private final Consumer<ConversionQueueItem> itemStartedListener;
    private final BiConsumer<
            ConversionQueueItem,
            SdmxConversionOutcome
            > itemFinishedListener;

    private final List<SdmxConversionOutcome> completedOutcomes =
            new ArrayList<>();

    /**
     * Creates a task for processing a conversion queue.
     *
     * @param conversionService conversion workflow service
     * @param installation active validated Converter installation
     * @param queueItems conversion queue items to process
     * @param itemStartedListener listener notified before processing each item
     * @param itemFinishedListener listener notified after processing each item
     */
    public ConversionBatchTask(
            SdmxConversionService conversionService,
            ConverterInstallation installation,
            List<ConversionQueueItem> queueItems,
            Consumer<ConversionQueueItem> itemStartedListener,
            BiConsumer<
                    ConversionQueueItem,
                    SdmxConversionOutcome
                    > itemFinishedListener
    ) {
        this.conversionService = Objects.requireNonNull(
                conversionService,
                "conversionService"
        );

        this.installation = Objects.requireNonNull(
                installation,
                "installation"
        );

        this.queueItems = List.copyOf(
                Objects.requireNonNull(
                        queueItems,
                        "queueItems"
                )
        );

        this.itemStartedListener = Objects.requireNonNull(
                itemStartedListener,
                "itemStartedListener"
        );

        this.itemFinishedListener = Objects.requireNonNull(
                itemFinishedListener,
                "itemFinishedListener"
        );

        if (this.queueItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "queueItems must not be empty"
            );
        }
    }

    /**
     * Returns an immutable snapshot of outcomes completed by the task.
     *
     * <p>This method can also be used after cancellation or an unexpected
     * failure to recover the results produced before the task stopped.</p>
     *
     * @return immutable snapshot of completed conversion outcomes
     */
    public List<SdmxConversionOutcome> completedOutcomes() {
        synchronized (completedOutcomes) {
            return List.copyOf(completedOutcomes);
        }
    }

    /**
     * Processes every conversion queue item sequentially.
     *
     * @return immutable collection of individual conversion outcomes
     * @throws InterruptedException if the background thread is interrupted
     */
    @Override
    protected List<SdmxConversionOutcome> call()
            throws InterruptedException {
        int totalItems = queueItems.size();

        updateProgress(0, totalItems);
        updateMessage("Preparando conversiones...");

        for (int index = 0; index < totalItems; index++) {
            if (isCancelled()) {
                break;
            }

            ConversionQueueItem queueItem =
                    queueItems.get(index);

            SdmxConversionRequest request =
                    queueItem.request();

            notifyItemStarted(queueItem);

            updateMessage(
                    "Convirtiendo "
                            + request.inputFile()
                            .getFileName()
            );

            SdmxConversionOutcome outcome;

            try {
                SdmxConversionResult result =
                        conversionService.convert(
                                installation,
                                request,
                                ignoredLine -> {
                                }
                        );

                outcome = SdmxConversionOutcome.completed(
                        result
                );
            } catch (InterruptedException exception) {
                if (isCancelled()) {
                    break;
                }

                throw exception;
            } catch (InvalidConversionRequestException
                     | IOException
                     | RuntimeException exception) {
                outcome = SdmxConversionOutcome.failed(
                        request,
                        exception
                );
            }

            addCompletedOutcome(outcome);

            notifyItemFinished(
                    queueItem,
                    outcome
            );

            updateProgress(index + 1, totalItems);
        }

        List<SdmxConversionOutcome> outcomes =
                completedOutcomes();

        if (isCancelled()) {
            updateMessage("Conversión cancelada.");
        } else {
            updateCompletionMessage(
                    outcomes,
                    totalItems
            );
        }

        return outcomes;
    }

    private void addCompletedOutcome(
            SdmxConversionOutcome outcome
    ) {
        synchronized (completedOutcomes) {
            completedOutcomes.add(outcome);
        }
    }

    private void notifyItemStarted(
            ConversionQueueItem queueItem
    ) {
        Platform.runLater(
                () -> itemStartedListener.accept(queueItem)
        );
    }

    private void notifyItemFinished(
            ConversionQueueItem queueItem,
            SdmxConversionOutcome outcome
    ) {
        Platform.runLater(
                () -> itemFinishedListener.accept(
                        queueItem,
                        outcome
                )
        );
    }

    private void updateCompletionMessage(
            List<SdmxConversionOutcome> outcomes,
            int totalItems
    ) {
        long successfulItems = outcomes.stream()
                .filter(SdmxConversionOutcome::isSuccessful)
                .count();

        if (successfulItems == totalItems) {
            updateMessage(
                    totalItems == 1
                            ? "Conversión completada correctamente."
                            : "Todas las conversiones se completaron "
                            + "correctamente."
            );
            return;
        }

        long failedItems = totalItems - successfulItems;

        updateMessage(
                "Proceso finalizado: "
                        + successfulItems
                        + " correctas y "
                        + failedItems
                        + " con errores."
        );
    }

}