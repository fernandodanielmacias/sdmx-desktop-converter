package io.github.ordonovus.sdmxconverter.application.conversion;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Contains the result of validating a generated SDMX-XML file.
 *
 * @param xmlFile validated XML file
 * @param fileSize file size in bytes
 * @param seriesCount number of generated Series elements
 * @param observationCount number of generated Obs elements
 * @param errors immutable collection of validation errors
 */
public record SdmxXmlValidationResult(
        Path xmlFile,
        long fileSize,
        long seriesCount,
        long observationCount,
        List<String> errors
) {

    /**
     * Validates and normalizes the XML validation result.
     */
    public SdmxXmlValidationResult {
        xmlFile = Objects.requireNonNull(
                xmlFile,
                "xmlFile"
        ).toAbsolutePath().normalize();

        errors = List.copyOf(
                Objects.requireNonNull(errors, "errors")
        );

        if (fileSize < 0) {
            throw new IllegalArgumentException(
                    "fileSize must not be negative"
            );
        }

        if (seriesCount < 0) {
            throw new IllegalArgumentException(
                    "seriesCount must not be negative"
            );
        }

        if (observationCount < 0) {
            throw new IllegalArgumentException(
                    "observationCount must not be negative"
            );
        }
    }

    /**
     * Indicates whether the generated XML passed every validation.
     *
     * @return {@code true} when no validation errors were found
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

}