package io.github.ordonovus.sdmxconverter.application.converter;

import java.util.List;
import java.util.Objects;

/**
 * Contains the errors and warnings produced while validating an SDMX
 * Converter installation.
 *
 * @param errors problems that prevent the converter from being used
 * @param warnings non-blocking conditions that should be reviewed
 */
public record ConverterInstallationValidationResult(
        List<String> errors,
        List<String> warnings
) {

    /**
     * Creates an immutable validation result.
     */
    public ConverterInstallationValidationResult {
        Objects.requireNonNull(errors, "errors");
        Objects.requireNonNull(warnings, "warnings");

        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    /**
     * Creates a successful validation result without warnings.
     *
     * @return successful validation result
     */
    public static ConverterInstallationValidationResult success() {
        return new ConverterInstallationValidationResult(
                List.of(),
                List.of()
        );
    }

    /**
     * Indicates whether the installation can be used.
     *
     * @return {@code true} when no blocking errors were found
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Indicates whether non-blocking warnings were found.
     *
     * @return {@code true} when at least one warning exists
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
