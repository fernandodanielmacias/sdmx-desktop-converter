package io.github.ordonovus.sdmxconverter.presentation.conversion;

import io.github.ordonovus.sdmxconverter.application.conversion.ExistingOutputPolicy;
import io.github.ordonovus.sdmxconverter.presentation.dialog.ExistingOutputDialog;
import io.github.ordonovus.sdmxconverter.presentation.log.ActivityLogManager;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogLevel;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionQueueItem;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates user decisions for conversion requests whose destination XML
 * files already exist.
 *
 * <p>The coordinator detects conflicts, displays the corresponding dialog,
 * prepares safe replacement requests or excludes existing outputs from the
 * conversion queue.</p>
 */
public final class ExistingOutputCoordinator {

    private static final String STATUS_SKIPPED = "Omitido";

    private final ExistingOutputDialog existingOutputDialog;
    private final ActivityLogManager activityLogManager;

    /**
     * Creates a coordinator for handling existing destination XML files.
     *
     * @param existingOutputDialog dialog used to request the user decision
     * @param activityLogManager manager used to report the selected action
     */
    public ExistingOutputCoordinator(
            ExistingOutputDialog existingOutputDialog,
            ActivityLogManager activityLogManager
    ) {
        this.existingOutputDialog = Objects.requireNonNull(
                existingOutputDialog,
                "existingOutputDialog"
        );

        this.activityLogManager = Objects.requireNonNull(
                activityLogManager,
                "activityLogManager"
        );
    }

    /**
     * Resolves existing destination XML conflicts in the supplied queue.
     *
     * <p>If no conflicts exist, the complete queue is returned unchanged.
     * An empty result indicates that the user cancelled the operation or that
     * every request was omitted.</p>
     *
     * @param owner owner window of the decision dialog
     * @param queueItems complete conversion queue
     * @return prepared queue, or empty when conversion must not start
     */
    public Optional<List<ConversionQueueItem>> prepare(
            Window owner,
            List<ConversionQueueItem> queueItems
    ) {
        Objects.requireNonNull(owner, "owner");

        List<ConversionQueueItem> safeQueueItems =
                List.copyOf(
                        Objects.requireNonNull(
                                queueItems,
                                "queueItems"
                        )
                );

        if (safeQueueItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "queueItems must not be empty"
            );
        }

        List<ConversionQueueItem> existingOutputItems =
                findExistingOutputItems(safeQueueItems);

        if (existingOutputItems.isEmpty()) {
            return Optional.of(safeQueueItems);
        }

        List<Path> existingOutputFiles =
                existingOutputItems.stream()
                        .map(item -> item.request().outputFile())
                        .toList();

        ExistingOutputDecision decision =
                existingOutputDialog.show(
                        owner,
                        existingOutputFiles
                );

        return switch (decision) {
            case REPLACE -> prepareReplacement(
                    safeQueueItems,
                    existingOutputItems
            );
            case SKIP -> prepareSkip(
                    safeQueueItems,
                    existingOutputItems
            );
            case CANCEL -> cancelConversion();
        };
    }

    private List<ConversionQueueItem> findExistingOutputItems(
            List<ConversionQueueItem> queueItems
    ) {
        return queueItems.stream()
                .filter(item -> Files.exists(
                        item.request().outputFile()
                ))
                .toList();
    }

    private Optional<List<ConversionQueueItem>> prepareReplacement(
            List<ConversionQueueItem> queueItems,
            List<ConversionQueueItem> existingOutputItems
    ) {
        List<ConversionQueueItem> preparedItems =
                queueItems.stream()
                        .map(item -> existingOutputItems.contains(item)
                                ? createReplacementItem(item)
                                : item)
                        .toList();

        activityLogManager.add(
                ActivityLogLevel.INFORMATION,
                existingOutputItems.size() == 1
                        ? "Se autorizó reemplazar 1 archivo XML existente."
                        : "Se autorizó reemplazar "
                        + existingOutputItems.size()
                        + " archivos XML existentes."
        );

        return Optional.of(preparedItems);
    }

    private ConversionQueueItem createReplacementItem(
            ConversionQueueItem queueItem
    ) {
        return new ConversionQueueItem(
                queueItem.row(),
                queueItem.request().withExistingOutputPolicy(
                        ExistingOutputPolicy.REPLACE_EXISTING
                )
        );
    }

    private Optional<List<ConversionQueueItem>> prepareSkip(
            List<ConversionQueueItem> queueItems,
            List<ConversionQueueItem> existingOutputItems
    ) {
        existingOutputItems.forEach(item -> {
            item.row().setStatus(STATUS_SKIPPED);
            item.row().clearConversionCounts();
        });

        List<ConversionQueueItem> pendingItems =
                queueItems.stream()
                        .filter(item -> !existingOutputItems.contains(item))
                        .toList();

        activityLogManager.add(
                ActivityLogLevel.WARNING,
                existingOutputItems.size() == 1
                        ? "Se omitió 1 archivo porque su XML de salida "
                        + "ya existe."
                        : "Se omitieron "
                        + existingOutputItems.size()
                        + " archivos porque sus XML de salida ya existen."
        );

        if (pendingItems.isEmpty()) {
            activityLogManager.add(
                    ActivityLogLevel.INFORMATION,
                    "No hay archivos pendientes para convertir."
            );

            return Optional.empty();
        }

        return Optional.of(pendingItems);
    }

    private Optional<List<ConversionQueueItem>> cancelConversion() {
        activityLogManager.add(
                ActivityLogLevel.INFORMATION,
                "La conversión se canceló antes de iniciar. "
                        + "No se modificó ningún archivo XML."
        );

        return Optional.empty();
    }

}