package io.github.ordonovus.sdmxconverter.presentation.conversion;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionOutcome;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains the technical information collected from one conversion batch.
 *
 * <p>The diagnostic preserves the converter installation, every requested
 * conversion, the outcomes completed before the batch stopped and its final
 * termination status.</p>
 *
 * @param installation converter installation used by the batch
 * @param requests immutable collection of requested conversions
 * @param outcomes immutable collection of completed conversion outcomes
 * @param startedAt instant when batch execution started
 * @param finishedAt instant when batch execution finished
 * @param status final batch status
 * @param unexpectedFailure unexpected task failure, when available
 */
public record ConversionBatchDiagnostic(
        ConverterInstallation installation,
        List<SdmxConversionRequest> requests,
        List<SdmxConversionOutcome> outcomes,
        Instant startedAt,
        Instant finishedAt,
        ConversionBatchStatus status,
        Optional<Throwable> unexpectedFailure
) {

    /**
     * Validates and creates an immutable batch diagnostic.
     */
    public ConversionBatchDiagnostic {
        Objects.requireNonNull(
                installation,
                "installation"
        );

        requests = List.copyOf(
                Objects.requireNonNull(requests, "requests")
        );

        outcomes = List.copyOf(
                Objects.requireNonNull(outcomes, "outcomes")
        );

        Objects.requireNonNull(
                startedAt,
                "startedAt"
        );

        Objects.requireNonNull(
                finishedAt,
                "finishedAt"
        );

        Objects.requireNonNull(
                status,
                "status"
        );

        Objects.requireNonNull(
                unexpectedFailure,
                "unexpectedFailure"
        );

        if (requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "requests must not be empty"
            );
        }

        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "finishedAt must not be before startedAt"
            );
        }

        if (outcomes.size() > requests.size()) {
            throw new IllegalArgumentException(
                    "outcomes must not exceed requests"
            );
        }

        if (status == ConversionBatchStatus.FAILED
                && unexpectedFailure.isEmpty()) {
            throw new IllegalArgumentException(
                    "A failed batch must contain an unexpected failure"
            );
        }

        if (status != ConversionBatchStatus.FAILED
                && unexpectedFailure.isPresent()) {
            throw new IllegalArgumentException(
                    "Only a failed batch may contain an unexpected failure"
            );
        }
    }

    /**
     * Creates a diagnostic for a completely processed batch.
     *
     * @param installation converter installation used by the batch
     * @param requests requested conversions
     * @param outcomes completed outcomes
     * @param startedAt batch start instant
     * @param finishedAt batch finish instant
     * @return completed batch diagnostic
     */
    public static ConversionBatchDiagnostic completed(
            ConverterInstallation installation,
            List<SdmxConversionRequest> requests,
            List<SdmxConversionOutcome> outcomes,
            Instant startedAt,
            Instant finishedAt
    ) {
        return new ConversionBatchDiagnostic(
                installation,
                requests,
                outcomes,
                startedAt,
                finishedAt,
                ConversionBatchStatus.COMPLETED,
                Optional.empty()
        );
    }

    /**
     * Creates a diagnostic for a user-cancelled batch.
     *
     * @param installation converter installation used by the batch
     * @param requests requested conversions
     * @param outcomes outcomes completed before cancellation
     * @param startedAt batch start instant
     * @param finishedAt batch finish instant
     * @return cancelled batch diagnostic
     */
    public static ConversionBatchDiagnostic cancelled(
            ConverterInstallation installation,
            List<SdmxConversionRequest> requests,
            List<SdmxConversionOutcome> outcomes,
            Instant startedAt,
            Instant finishedAt
    ) {
        return new ConversionBatchDiagnostic(
                installation,
                requests,
                outcomes,
                startedAt,
                finishedAt,
                ConversionBatchStatus.CANCELLED,
                Optional.empty()
        );
    }

    /**
     * Creates a diagnostic for an unexpectedly failed batch.
     *
     * @param installation converter installation used by the batch
     * @param requests requested conversions
     * @param outcomes outcomes completed before the failure
     * @param startedAt batch start instant
     * @param finishedAt batch finish instant
     * @param failure unexpected task failure
     * @return failed batch diagnostic
     */
    public static ConversionBatchDiagnostic failed(
            ConverterInstallation installation,
            List<SdmxConversionRequest> requests,
            List<SdmxConversionOutcome> outcomes,
            Instant startedAt,
            Instant finishedAt,
            Throwable failure
    ) {
        return new ConversionBatchDiagnostic(
                installation,
                requests,
                outcomes,
                startedAt,
                finishedAt,
                ConversionBatchStatus.FAILED,
                Optional.of(
                        Objects.requireNonNull(failure, "failure")
                )
        );
    }

    /**
     * Returns the total execution duration of the batch.
     *
     * @return elapsed batch duration
     */
    public Duration duration() {
        return Duration.between(startedAt, finishedAt);
    }

    /**
     * Returns the number of outcomes produced by the batch.
     *
     * @return processed conversion count
     */
    public int processedCount() {
        return outcomes.size();
    }

    /**
     * Returns the number of successful conversion outcomes.
     *
     * @return successful conversion count
     */
    public long successfulCount() {
        return outcomes.stream()
                .filter(SdmxConversionOutcome::isSuccessful)
                .count();
    }

    /**
     * Returns the number of unsuccessful conversion outcomes.
     *
     * @return unsuccessful conversion count
     */
    public long failedCount() {
        return outcomes.stream()
                .filter(outcome -> !outcome.isSuccessful())
                .count();
    }

    /**
     * Returns the number of requests that did not produce an outcome.
     *
     * <p>This can occur when a batch is cancelled or fails unexpectedly.</p>
     *
     * @return unprocessed request count
     */
    public int unprocessedCount() {
        return requests.size() - outcomes.size();
    }

}