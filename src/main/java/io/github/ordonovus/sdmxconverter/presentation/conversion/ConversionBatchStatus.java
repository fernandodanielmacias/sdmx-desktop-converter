package io.github.ordonovus.sdmxconverter.presentation.conversion;

/**
 * Identifies how a conversion batch finished.
 */
public enum ConversionBatchStatus {

    /**
     * Every queued item was processed, although individual conversions may
     * have completed successfully or with errors.
     */
    COMPLETED,

    /**
     * The user cancelled the batch before every queued item was processed.
     */
    CANCELLED,

    /**
     * An unexpected failure prevented the batch task from continuing.
     */
    FAILED
}