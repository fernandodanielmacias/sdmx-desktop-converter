package io.github.ordonovus.sdmxconverter.application.conversion;

import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents all information required to perform one Excel to SDMX-XML
 * conversion.
 *
 * @param inputFile Excel input file
 * @param outputFile destination XML file
 * @param dsdFile SDMX structure file
 * @param headerFile SDMX header properties file
 * @param dsdMetadata metadata identifying the selected DSD
 * @param parameters general Converter execution parameters
 */
public record SdmxConversionRequest(
        Path inputFile,
        Path outputFile,
        Path dsdFile,
        Path headerFile,
        DsdMetadata dsdMetadata,
        SdmxConversionParameters parameters
) {

    /**
     * Validates and normalizes every path in the request.
     */
    public SdmxConversionRequest {
        inputFile = normalizePath(
                inputFile,
                "inputFile"
        );

        outputFile = normalizePath(
                outputFile,
                "outputFile"
        );

        dsdFile = normalizePath(
                dsdFile,
                "dsdFile"
        );

        headerFile = normalizePath(
                headerFile,
                "headerFile"
        );

        dsdMetadata = Objects.requireNonNull(
                dsdMetadata,
                "dsdMetadata"
        );

        parameters = Objects.requireNonNull(
                parameters,
                "parameters"
        );
    }

    private static Path normalizePath(
            Path path,
            String fieldName
    ) {
        return Objects.requireNonNull(
                path,
                fieldName
        ).toAbsolutePath().normalize();
    }

}