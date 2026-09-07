package io.github.ordonovus.sdmxconverter.application.conversion;

import java.util.List;
import java.util.Objects;

/**
 * Contains the errors found while validating a conversion request.
 *
 * @param errors immutable collection of validation errors
 */
public record ConversionRequestValidationResult(
        List<String> errors
) {

    /**
     * Creates an immutable validation result.
     */
    public ConversionRequestValidationResult {
        errors = List.copyOf(
                Objects.requireNonNull(errors, "errors")
        );
    }

    /**
     * Indicates whether the conversion request passed every validation.
     *
     * @return {@code true} when no validation errors were found
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

}