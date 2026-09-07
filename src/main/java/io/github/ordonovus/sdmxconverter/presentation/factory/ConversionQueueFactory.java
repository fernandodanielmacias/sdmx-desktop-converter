package io.github.ordonovus.sdmxconverter.presentation.factory;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionParameters;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionFileRow;
import io.github.ordonovus.sdmxconverter.presentation.model.ConversionQueueItem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Creates the conversion queue from the files and configuration selected
 * in the user interface.
 */
public final class ConversionQueueFactory {

    /**
     * Creates the conversion queue.
     *
     * @param rows file rows displayed in the main table
     * @param outputDirectory selected output directory
     * @param dsdFile selected DSD file
     * @param headerFile selected header properties file
     * @param dsdMetadata metadata extracted from the DSD
     * @param parameters general conversion parameters
     * @return immutable collection of conversion queue items
     */
    public List<ConversionQueueItem> create(
            List<ConversionFileRow> rows,
            Path outputDirectory,
            Path dsdFile,
            Path headerFile,
            DsdMetadata dsdMetadata,
            SdmxConversionParameters parameters
    ) {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(
                outputDirectory,
                "outputDirectory"
        );
        Objects.requireNonNull(dsdFile, "dsdFile");
        Objects.requireNonNull(headerFile, "headerFile");
        Objects.requireNonNull(dsdMetadata, "dsdMetadata");
        Objects.requireNonNull(parameters, "parameters");

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "rows must not be empty"
            );
        }

        Path normalizedOutputDirectory =
                outputDirectory.toAbsolutePath().normalize();

        List<ConversionQueueItem> queue =
                new ArrayList<>();

        Set<Path> outputFiles = new HashSet<>();

        for (ConversionFileRow row : rows) {
            ConversionFileRow validatedRow =
                    Objects.requireNonNull(
                            row,
                            "rows must not contain null values"
                    );

            Path outputFile = createOutputFile(
                    normalizedOutputDirectory,
                    validatedRow.getOutputFileName()
            );

            if (!outputFiles.add(outputFile)) {
                throw new IllegalArgumentException(
                        "Multiple rows resolve to the same output file: "
                                + outputFile
                );
            }

            SdmxConversionRequest request =
                    new SdmxConversionRequest(
                            validatedRow.getPath(),
                            outputFile,
                            dsdFile,
                            headerFile,
                            dsdMetadata,
                            parameters
                    );

            queue.add(
                    new ConversionQueueItem(
                            validatedRow,
                            request
                    )
            );
        }

        return List.copyOf(queue);
    }

    private Path createOutputFile(
            Path outputDirectory,
            String outputFileName
    ) {
        String normalizedFileName =
                Objects.requireNonNull(
                        outputFileName,
                        "outputFileName"
                ).trim();

        if (normalizedFileName.isEmpty()) {
            throw new IllegalArgumentException(
                    "outputFileName must not be blank"
            );
        }

        Path relativeOutputFile =
                Path.of(normalizedFileName);

        if (relativeOutputFile.isAbsolute()
                || relativeOutputFile.getNameCount() != 1) {
            throw new IllegalArgumentException(
                    "outputFileName must contain only a file name"
            );
        }

        Path outputFile = outputDirectory
                .resolve(relativeOutputFile)
                .normalize();

        if (!outputFile.startsWith(outputDirectory)) {
            throw new IllegalArgumentException(
                    "The output file must remain inside "
                            + "the selected output directory"
            );
        }

        return outputFile;
    }

}