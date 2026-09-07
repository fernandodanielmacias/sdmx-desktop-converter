package io.github.ordonovus.sdmxconverter.application.conversion;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the outcome of processing one conversion request.
 *
 * <p>An outcome contains either a completed conversion result or an
 * exception that prevented the conversion workflow from completing.</p>
 */
public final class SdmxConversionOutcome {

    private final SdmxConversionRequest request;
    private final SdmxConversionResult result;
    private final Throwable failure;

    private SdmxConversionOutcome(
            SdmxConversionRequest request,
            SdmxConversionResult result,
            Throwable failure
    ) {
        this.request = Objects.requireNonNull(
                request,
                "request"
        );
        this.result = result;
        this.failure = failure;

        boolean hasResult = result != null;
        boolean hasFailure = failure != null;

        if (hasResult == hasFailure) {
            throw new IllegalArgumentException(
                    "An outcome must contain either a result "
                            + "or a failure"
            );
        }

        if (hasResult && !request.equals(result.request())) {
            throw new IllegalArgumentException(
                    "The result does not belong to the request"
            );
        }
    }

    /**
     * Creates an outcome for a completed conversion workflow.
     *
     * <p>The contained result may still indicate that the external process
     * or generated XML was unsuccessful.</p>
     *
     * @param result complete conversion result
     * @return completed conversion outcome
     */
    public static SdmxConversionOutcome completed(
            SdmxConversionResult result
    ) {
        Objects.requireNonNull(result, "result");

        return new SdmxConversionOutcome(
                result.request(),
                result,
                null
        );
    }

    /**
     * Creates an outcome for a conversion that could not complete.
     *
     * @param request conversion request
     * @param failure exception that interrupted the conversion workflow
     * @return failed conversion outcome
     */
    public static SdmxConversionOutcome failed(
            SdmxConversionRequest request,
            Throwable failure
    ) {
        return new SdmxConversionOutcome(
                request,
                null,
                Objects.requireNonNull(
                        failure,
                        "failure"
                )
        );
    }

    /**
     * Returns the processed conversion request.
     *
     * @return conversion request
     */
    public SdmxConversionRequest request() {
        return request;
    }

    /**
     * Returns the completed conversion result when available.
     *
     * @return optional conversion result
     */
    public Optional<SdmxConversionResult> result() {
        return Optional.ofNullable(result);
    }

    /**
     * Returns the exception that prevented completion when available.
     *
     * @return optional execution failure
     */
    public Optional<Throwable> failure() {
        return Optional.ofNullable(failure);
    }

    /**
     * Indicates whether the conversion and generated XML were successful.
     *
     * @return {@code true} only for a fully successful conversion
     */
    public boolean isSuccessful() {
        return result != null && result.isSuccessful();
    }

}