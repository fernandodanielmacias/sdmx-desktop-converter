package io.github.ordonovus.sdmxconverter.presentation.model;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;

import java.util.Objects;

/**
 * Associates a table row with the conversion request created from it.
 *
 * @param row user-interface row displayed in the conversion queue
 * @param request conversion request represented by the row
 */
public record ConversionQueueItem(
        ConversionFileRow row,
        SdmxConversionRequest request
) {

    /**
     * Validates that the row and request represent the same input file.
     */
    public ConversionQueueItem {
        Objects.requireNonNull(row, "row");
        Objects.requireNonNull(request, "request");

        if (!row.getPath().equals(request.inputFile())) {
            throw new IllegalArgumentException(
                    "The queue row and request must reference "
                            + "the same input file"
            );
        }
    }

}