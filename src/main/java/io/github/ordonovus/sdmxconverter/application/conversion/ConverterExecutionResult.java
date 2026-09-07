package io.github.ordonovus.sdmxconverter.application.conversion;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Contains the technical result returned by one SDMX Converter process.
 *
 * @param exitCode operating-system process exit code
 * @param startedAt process start time
 * @param finishedAt process completion time
 * @param outputLines standard and error output captured from the process
 */
public record ConverterExecutionResult(
        int exitCode,
        Instant startedAt,
        Instant finishedAt,
        List<String> outputLines
) {

    /**
     * Validates the execution timestamps and creates an immutable output
     * collection.
     */
    public ConverterExecutionResult {
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(finishedAt, "finishedAt");

        outputLines = List.copyOf(
                Objects.requireNonNull(
                        outputLines,
                        "outputLines"
                )
        );

        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "finishedAt must not be before startedAt"
            );
        }
    }

    /**
     * Indicates whether the external process completed successfully.
     *
     * @return {@code true} when the process returned exit code zero
     */
    public boolean isSuccessful() {
        return exitCode == 0;
    }

    /**
     * Returns the total process execution duration.
     *
     * @return elapsed execution time
     */
    public Duration duration() {
        return Duration.between(startedAt, finishedAt);
    }

    /**
     * Joins all captured process messages into one technical text.
     *
     * @return complete captured process output
     */
    public String technicalOutput() {
        return String.join(
                System.lineSeparator(),
                outputLines
        );
    }

}