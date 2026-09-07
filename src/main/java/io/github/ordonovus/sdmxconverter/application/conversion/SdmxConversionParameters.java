package io.github.ordonovus.sdmxconverter.application.conversion;

import java.util.Locale;
import java.util.Objects;

/**
 * Defines the general parameters used by the SDMX Converter.
 *
 * <p>These values represent the validated baseline configuration for
 * converting Excel files to compact SDMX-XML.</p>
 *
 * @param validation indicates whether SDMX validation is enabled
 * @param inputFormat input data format
 * @param outputFormat output data format
 * @param maximumErrorCount maximum number of validation errors
 * @param useRegistry indicates whether the SDMX Registry is used
 * @param errorIfEmpty indicates whether empty input data is an error
 * @param groupedOutput indicates whether observations are grouped by series
 */
public record SdmxConversionParameters(
        boolean validation,
        String inputFormat,
        String outputFormat,
        int maximumErrorCount,
        boolean useRegistry,
        boolean errorIfEmpty,
        boolean groupedOutput
) {

    private static final int DEFAULT_MAXIMUM_ERROR_COUNT = 100;

    /**
     * Validates and normalizes the conversion parameters.
     */
    public SdmxConversionParameters {
        inputFormat = normalizeFormat(
                inputFormat,
                "inputFormat"
        );

        outputFormat = normalizeFormat(
                outputFormat,
                "outputFormat"
        );

        if (maximumErrorCount <= 0) {
            throw new IllegalArgumentException(
                    "maximumErrorCount must be greater than zero"
            );
        }
    }

    /**
     * Creates the initial parameters validated for the application.
     *
     * @return validated baseline conversion parameters
     */
    public static SdmxConversionParameters defaults() {
        return new SdmxConversionParameters(
                true,
                "EXCEL",
                "COMPACT_SDMX",
                DEFAULT_MAXIMUM_ERROR_COUNT,
                false,
                true,
                true
        );
    }

    private static String normalizeFormat(
            String value,
            String fieldName
    ) {
        String normalizedValue = Objects.requireNonNull(
                value,
                fieldName
        ).trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return normalizedValue.toUpperCase(Locale.ROOT);
    }

}