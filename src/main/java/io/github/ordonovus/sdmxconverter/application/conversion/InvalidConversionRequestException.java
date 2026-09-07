package io.github.ordonovus.sdmxconverter.application.conversion;

import java.util.Objects;

/**
 * Indicates that a conversion request failed its pre-execution
 * validations.
 */
public final class InvalidConversionRequestException extends Exception {

    private final ConversionRequestValidationResult validationResult;

    /**
     * Creates an exception containing every request validation error.
     *
     * @param validationResult invalid request validation result
     */
    public InvalidConversionRequestException(
            ConversionRequestValidationResult validationResult
    ) {
        super(createMessage(validationResult));

        this.validationResult = validationResult;
    }

    /**
     * Returns the complete request validation result.
     *
     * @return request validation result
     */
    public ConversionRequestValidationResult getValidationResult() {
        return validationResult;
    }

    private static String createMessage(
            ConversionRequestValidationResult validationResult
    ) {
        Objects.requireNonNull(
                validationResult,
                "validationResult"
        );

        if (validationResult.isValid()) {
            throw new IllegalArgumentException(
                    "validationResult must contain at least one error"
            );
        }

        return "Conversion request validation failed: "
                + String.join(
                "; ",
                validationResult.errors()
        );
    }

}