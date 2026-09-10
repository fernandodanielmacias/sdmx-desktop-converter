package io.github.ordonovus.sdmxconverter.presentation.log;

import io.github.ordonovus.sdmxconverter.application.conversion.ConverterExecutionResult;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionOutcome;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionParameters;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionResult;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxXmlValidationResult;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.presentation.conversion.ConversionBatchDiagnostic;
import io.github.ordonovus.sdmxconverter.presentation.conversion.ConversionBatchStatus;
import io.github.ordonovus.sdmxconverter.presentation.model.ActivityLogEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
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
 * Exports the visual activity history and conversion diagnostics to a UTF-8
 * log file.
 */
public final class ActivityLogFileExporter {

    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final DateTimeFormatter EXPORTED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exports only the supplied visual activity entries.
     *
     * <p>This overload is retained for callers that do not yet provide
     * conversion diagnostics.</p>
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
        return export(
                directory,
                entries,
                List.of()
        );
    }

    /**
     * Exports the activity history and conversion diagnostics.
     *
     * <p>The generated file name contains the current date and time. If a file
     * with the same name already exists, a numeric suffix is appended.</p>
     *
     * @param directory directory selected by the user
     * @param entries activity entries to export
     * @param diagnostics conversion batch diagnostics to export
     * @return path of the generated log file
     * @throws IOException if the directory is invalid or the file cannot be
     *                     written
     */
    public Path export(
            Path directory,
            List<ActivityLogEntry> entries,
            List<ConversionBatchDiagnostic> diagnostics
    ) throws IOException {
        Path normalizedDirectory = Objects.requireNonNull(
                directory,
                "directory"
        ).toAbsolutePath().normalize();

        List<ActivityLogEntry> safeEntries = List.copyOf(
                Objects.requireNonNull(entries, "entries")
        );

        List<ConversionBatchDiagnostic> safeDiagnostics =
                List.copyOf(
                        Objects.requireNonNull(
                                diagnostics,
                                "diagnostics"
                        )
                );

        validateDirectory(normalizedDirectory);

        LocalDateTime exportedAt = LocalDateTime.now();

        Path logFile = createAvailableFilePath(
                normalizedDirectory,
                exportedAt
        );

        String content = createLogContent(
                safeEntries,
                safeDiagnostics,
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
        String timestamp =
                FILE_NAME_FORMATTER.format(exportedAt);

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
            List<ConversionBatchDiagnostic> diagnostics,
            LocalDateTime exportedAt
    ) {
        var content = new StringBuilder();

        appendLine(
                content,
                "Convertidor SDMX - Registro de diagnóstico"
        );
        appendLine(
                content,
                "Exportado: "
                        + EXPORTED_AT_FORMATTER.format(exportedAt)
        );
        appendLine(
                content,
                "Entradas de actividad: " + entries.size()
        );
        appendLine(
                content,
                "Lotes registrados: " + diagnostics.size()
        );
        appendLine(content, "");

        appendEnvironmentSection(content);
        appendActivitySection(content, entries);
        appendDiagnosticSection(content, diagnostics);

        return content.toString();
    }

    private void appendEnvironmentSection(
            StringBuilder content
    ) {
        appendSectionTitle(
                content,
                "INFORMACIÓN DEL ENTORNO"
        );

        appendProperty(
                content,
                "Sistema operativo",
                System.getProperty("os.name")
        );
        appendProperty(
                content,
                "Versión del sistema operativo",
                System.getProperty("os.version")
        );
        appendProperty(
                content,
                "Arquitectura",
                System.getProperty("os.arch")
        );
        appendProperty(
                content,
                "Java de la aplicación",
                System.getProperty("java.version")
        );
        appendProperty(
                content,
                "Proveedor de Java",
                System.getProperty("java.vendor")
        );
        appendProperty(
                content,
                "Codificación predeterminada",
                Charset.defaultCharset().displayName()
        );

        appendLine(content, "");
    }

    private void appendActivitySection(
            StringBuilder content,
            List<ActivityLogEntry> entries
    ) {
        appendSectionTitle(
                content,
                "HISTORIAL DE ACTIVIDAD"
        );

        if (entries.isEmpty()) {
            appendLine(
                    content,
                    "(sin entradas de actividad)"
            );
        } else {
            String activityContent = entries.stream()
                    .map(this::formatEntry)
                    .collect(
                            Collectors.joining(
                                    System.lineSeparator()
                            )
                    );

            appendLine(content, activityContent);
        }

        appendLine(content, "");
    }

    private void appendDiagnosticSection(
            StringBuilder content,
            List<ConversionBatchDiagnostic> diagnostics
    ) {
        appendSectionTitle(
                content,
                "DIAGNÓSTICO DE CONVERSIONES"
        );

        if (diagnostics.isEmpty()) {
            appendLine(
                    content,
                    "(no se registraron conversiones en esta sesión)"
            );
            appendLine(content, "");
            return;
        }

        for (int index = 0;
             index < diagnostics.size();
             index++) {
            appendBatchDiagnostic(
                    content,
                    diagnostics.get(index),
                    index + 1
            );
        }
    }

    private void appendBatchDiagnostic(
            StringBuilder content,
            ConversionBatchDiagnostic diagnostic,
            int batchNumber
    ) {
        appendLine(
                content,
                "LOTE " + batchNumber
        );
        appendLine(
                content,
                "------"
        );

        appendProperty(
                content,
                "Estado final",
                formatBatchStatus(diagnostic.status())
        );
        appendProperty(
                content,
                "Inicio",
                diagnostic.startedAt().toString()
        );
        appendProperty(
                content,
                "Fin",
                diagnostic.finishedAt().toString()
        );
        appendProperty(
                content,
                "Duración",
                diagnostic.duration().toMillis() + " ms"
        );
        appendProperty(
                content,
                "Solicitudes",
                diagnostic.requests().size()
        );
        appendProperty(
                content,
                "Procesadas",
                diagnostic.processedCount()
        );
        appendProperty(
                content,
                "Correctas",
                diagnostic.successfulCount()
        );
        appendProperty(
                content,
                "Con errores",
                diagnostic.failedCount()
        );
        appendProperty(
                content,
                "Sin procesar",
                diagnostic.unprocessedCount()
        );

        appendInstallation(
                content,
                diagnostic.installation()
        );

        diagnostic.unexpectedFailure().ifPresent(
                failure -> {
                    appendLine(content, "");
                    appendLine(
                            content,
                            "FALLO INESPERADO DEL LOTE"
                    );
                    appendFailure(content, failure);
                }
        );

        appendLine(content, "");

        for (int index = 0;
             index < diagnostic.requests().size();
             index++) {
            SdmxConversionRequest request =
                    diagnostic.requests().get(index);

            SdmxConversionOutcome outcome =
                    findOutcome(
                            diagnostic.outcomes(),
                            request
                    );

            appendConversionDiagnostic(
                    content,
                    request,
                    outcome,
                    index + 1
            );
        }

        appendLine(
                content,
                "FIN DEL LOTE " + batchNumber
        );
        appendLine(content, "");
    }

    private void appendInstallation(
            StringBuilder content,
            ConverterInstallation installation
    ) {
        appendLine(content, "");
        appendLine(
                content,
                "CONFIGURACIÓN DEL CONVERTIDOR"
        );

        appendProperty(
                content,
                "Versión del convertidor",
                installation.converterVersion()
        );
        appendProperty(
                content,
                "Java requerido",
                installation.requiredJavaMajorVersion()
        );
        appendProperty(
                content,
                "Carpeta de instalación",
                installation.installationDirectory()
        );
        appendProperty(
                content,
                "Ejecutable privado de Java",
                installation.javaExecutable()
        );
        appendProperty(
                content,
                "JAR principal",
                installation.launcherJar()
        );
    }

    private void appendConversionDiagnostic(
            StringBuilder content,
            SdmxConversionRequest request,
            SdmxConversionOutcome outcome,
            int conversionNumber
    ) {
        appendLine(
                content,
                "CONVERSIÓN " + conversionNumber
        );
        appendLine(
                content,
                "------------"
        );

        appendRequest(content, request);

        if (outcome == null) {
            appendProperty(
                    content,
                    "Resultado",
                    "Sin procesar o interrumpida antes de generar resultado"
            );
            appendLine(content, "");
            return;
        }

        outcome.result().ifPresentOrElse(
                result -> appendResult(content, result),
                () -> appendExecutionFailure(
                        content,
                        outcome
                )
        );

        appendLine(content, "");
    }

    private void appendRequest(
            StringBuilder content,
            SdmxConversionRequest request
    ) {
        appendProperty(
                content,
                "Archivo de entrada",
                request.inputFile()
        );
        appendProperty(
                content,
                "Archivo de salida",
                request.outputFile()
        );
        appendProperty(
                content,
                "Archivo DSD",
                request.dsdFile()
        );
        appendProperty(
                content,
                "Archivo de encabezado",
                request.headerFile()
        );
        appendProperty(
                content,
                "Identidad del DSD",
                request.dsdMetadata().formattedIdentity()
        );

        appendParameters(
                content,
                request.parameters()
        );
    }

    private void appendParameters(
            StringBuilder content,
            SdmxConversionParameters parameters
    ) {
        appendProperty(
                content,
                "Validación habilitada",
                parameters.validation()
        );
        appendProperty(
                content,
                "Formato de entrada",
                parameters.inputFormat()
        );
        appendProperty(
                content,
                "Formato de salida",
                parameters.outputFormat()
        );
        appendProperty(
                content,
                "Máximo de errores",
                parameters.maximumErrorCount()
        );
        appendProperty(
                content,
                "Uso de Registry",
                parameters.useRegistry()
        );
        appendProperty(
                content,
                "Error si está vacío",
                parameters.errorIfEmpty()
        );
        appendProperty(
                content,
                "Salida agrupada",
                parameters.groupedOutput()
        );
    }

    private void appendResult(
            StringBuilder content,
            SdmxConversionResult result
    ) {
        appendProperty(
                content,
                "Resultado",
                result.isSuccessful()
                        ? "Correcto"
                        : "Con errores"
        );
        appendProperty(
                content,
                "Duración total",
                result.duration().toMillis() + " ms"
        );

        appendExecutionResult(
                content,
                result.executionResult()
        );

        appendXmlValidationResult(
                content,
                result.xmlValidationResult()
        );
    }

    private void appendExecutionResult(
            StringBuilder content,
            ConverterExecutionResult executionResult
    ) {
        appendLine(content, "");
        appendLine(
                content,
                "EJECUCIÓN DEL CONVERTIDOR"
        );

        appendProperty(
                content,
                "Código de salida",
                executionResult.exitCode()
        );
        appendProperty(
                content,
                "Inicio del proceso",
                executionResult.startedAt()
        );
        appendProperty(
                content,
                "Fin del proceso",
                executionResult.finishedAt()
        );
        appendProperty(
                content,
                "Duración del proceso",
                executionResult.duration().toMillis() + " ms"
        );

        appendLine(content, "");
        appendLine(
                content,
                "SALIDA CAPTURADA DEL CONVERTER (STDOUT + STDERR)"
        );

        if (executionResult.outputLines().isEmpty()) {
            appendLine(
                    content,
                    "(sin salida capturada)"
            );
            return;
        }

        for (String outputLine :
                executionResult.outputLines()) {
            appendLine(content, outputLine);
        }
    }

    private void appendXmlValidationResult(
            StringBuilder content,
            SdmxXmlValidationResult validationResult
    ) {
        appendLine(content, "");
        appendLine(
                content,
                "VALIDACIÓN DEL XML GENERADO"
        );

        appendProperty(
                content,
                "Archivo XML",
                validationResult.xmlFile()
        );
        appendProperty(
                content,
                "Integridad básica",
                validationResult.isValid()
                        ? "Correcta"
                        : "Con errores"
        );
        appendProperty(
                content,
                "Tamaño",
                validationResult.fileSize() + " bytes"
        );
        appendProperty(
                content,
                "Series",
                validationResult.seriesCount()
        );
        appendProperty(
                content,
                "Observaciones",
                validationResult.observationCount()
        );

        if (validationResult.errors().isEmpty()) {
            appendProperty(
                    content,
                    "Errores de validación",
                    "Ninguno"
            );
            return;
        }

        appendLine(
                content,
                "Errores de validación:"
        );

        for (String error : validationResult.errors()) {
            appendLine(
                    content,
                    "  - " + error
            );
        }
    }

    private void appendExecutionFailure(
            StringBuilder content,
            SdmxConversionOutcome outcome
    ) {
        appendProperty(
                content,
                "Resultado",
                "No se pudo completar la ejecución"
        );

        Throwable failure = outcome.failure()
                .orElseThrow();

        appendLine(content, "");
        appendLine(
                content,
                "EXCEPCIÓN DE LA CONVERSIÓN"
        );
        appendFailure(content, failure);
    }

    private SdmxConversionOutcome findOutcome(
            List<SdmxConversionOutcome> outcomes,
            SdmxConversionRequest request
    ) {
        return outcomes.stream()
                .filter(outcome -> outcome.request().equals(request))
                .findFirst()
                .orElse(null);
    }

    private void appendFailure(
            StringBuilder content,
            Throwable failure
    ) {
        appendProperty(
                content,
                "Tipo",
                failure.getClass().getName()
        );
        appendProperty(
                content,
                "Mensaje",
                requireFailureMessage(failure)
        );

        appendLine(
                content,
                "Stack trace:"
        );

        var writer = new StringWriter();

        try (var printWriter = new PrintWriter(writer)) {
            failure.printStackTrace(printWriter);
        }

        appendLine(
                content,
                writer.toString().stripTrailing()
        );
    }

    private String formatBatchStatus(
            ConversionBatchStatus status
    ) {
        return switch (status) {
            case COMPLETED -> "Completado";
            case CANCELLED -> "Cancelado";
            case FAILED -> "Fallido inesperadamente";
        };
    }

    private String requireFailureMessage(
            Throwable failure
    ) {
        String message = failure.getMessage();

        return message == null || message.isBlank()
                ? "(sin mensaje)"
                : message;
    }

    private String formatEntry(
            ActivityLogEntry entry
    ) {
        return "%s  %-11s  %s".formatted(
                entry.formattedTime(),
                entry.level().getDisplayName(),
                entry.message()
        );
    }

    private void appendSectionTitle(
            StringBuilder content,
            String title
    ) {
        appendLine(
                content,
                "============================================================"
        );
        appendLine(content, title);
        appendLine(
                content,
                "============================================================"
        );
    }

    private void appendProperty(
            StringBuilder content,
            String name,
            Object value
    ) {
        appendLine(
                content,
                name + ": " + value
        );
    }

    private void appendLine(
            StringBuilder content,
            String value
    ) {
        content.append(value)
                .append(System.lineSeparator());
    }

}
