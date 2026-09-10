package io.github.ordonovus.sdmxconverter.presentation.log;

import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Exports the visual activity history to a UTF-8 diagnostic log file.
 */
public final class ActivityLogFileExporter {

    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final DateTimeFormatter EXPORTED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exports the supplied activity entries to the selected directory.
     *
     * <p>The generated file name contains the current date and time. If a file
     * with the same name already exists, a numeric suffix is appended.</p>
     *
     * @param directory directory selected by the user
     * @param entries activity entries to export
     * @return path of the generated log file
     * @throws IOException if the directory is invalid or the file cannot be
     *                     written
     */
    public Path export(
            Path directory,
            List<ActivityLogEntry> entries
    ) throws IOException {
        Path normalizedDirectory = Objects.requireNonNull(
                directory,
                "directory"
        ).toAbsolutePath().normalize();

        List<ActivityLogEntry> safeEntries = List.copyOf(
                Objects.requireNonNull(entries, "entries")
        );

        validateDirectory(normalizedDirectory);

        LocalDateTime exportedAt = LocalDateTime.now();
        Path logFile = createAvailableFilePath(
                normalizedDirectory,
                exportedAt
        );

        String content = createLogContent(
                safeEntries,
                exportedAt
        );

        Files.writeString(
                logFile,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return logFile;
    }

    private void validateDirectory(Path directory)
            throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException(
                    "The selected log directory does not exist"
            );
        }

        if (!Files.isWritable(directory)) {
            throw new IOException(
                    "The selected log directory is not writable"
            );
        }
    }

    private Path createAvailableFilePath(
            Path directory,
            LocalDateTime exportedAt
    ) {
        String timestamp = FILE_NAME_FORMATTER.format(exportedAt);
        String baseName = "sdmx-converter_"
                + timestamp;

        Path candidate = directory.resolve(
                baseName + ".log"
        );

        int suffix = 2;

        while (Files.exists(candidate)) {
            candidate = directory.resolve(
                    baseName + "_" + suffix + ".log"
            );
            suffix++;
        }

        return candidate;
    }

    private String createLogContent(
            List<ActivityLogEntry> entries,
            LocalDateTime exportedAt
    ) {
        String lineSeparator = System.lineSeparator();

        String activityContent = entries.stream()
                .map(this::formatEntry)
                .collect(
                        Collectors.joining(lineSeparator)
                );

        return String.join(
                lineSeparator,
                "Convertidor SDMX - Registro de actividad",
                "Exportado: "
                        + EXPORTED_AT_FORMATTER.format(exportedAt),
                "Entradas registradas: " + entries.size(),
                "",
                activityContent,
                ""
        );
    }

    private String formatEntry(ActivityLogEntry entry) {
        return "%s  %-11s  %s".formatted(
                entry.formattedTime(),
                entry.level().getDisplayName(),
                entry.message()
        );
    }
}