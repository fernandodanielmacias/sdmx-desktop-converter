package io.github.ordonovus.sdmxconverter.application.conversion;

import java.time.Duration;
import java.util.Objects;

/**
 * Combines the external Converter process result with the validation of
 * the generated SDMX-XML file.
 *
 * @param request executed conversion request
 * @param executionResult external process execution result
 * @param xmlValidationResult generated XML validation result
 */
public record SdmxConversionResult(
        SdmxConversionRequest request,
        ConverterExecutionResult executionResult,
        SdmxXmlValidationResult xmlValidationResult
) {

    /**
     * Creates a complete conversion result.
     */
    public SdmxConversionResult {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(
                executionResult,
                "executionResult"
        );
        Objects.requireNonNull(
                xmlValidationResult,
                "xmlValidationResult"
        );

        if (!request.outputFile().equals(
                xmlValidationResult.xmlFile()
        )) {
            throw new IllegalArgumentException(
                    "The validated XML file does not match "
                            + "the requested output file"
            );
        }
    }

    /**
     * Indicates whether both the external process and generated XML
     * completed successfully.
     *
     * @return {@code true} when the process returned zero and the XML is valid
     */
    public boolean isSuccessful() {
        return executionResult.isSuccessful()
                && xmlValidationResult.isValid();
    }

    /**
     * Returns the external process execution duration.
     *
     * @return conversion process duration
     */
    public Duration duration() {
        return executionResult.duration();
    }

    /**
     * Returns the number of generated SDMX series.
     *
     * @return generated Series element count
     */
    public long seriesCount() {
        return xmlValidationResult.seriesCount();
    }

    /**
     * Returns the number of generated observations.
     *
     * @return generated Obs element count
     */
    public long observationCount() {
        return xmlValidationResult.observationCount();
    }

}