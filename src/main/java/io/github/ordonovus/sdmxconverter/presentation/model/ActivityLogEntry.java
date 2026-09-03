package io.github.ordonovus.sdmxconverter.presentation.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a timestamped message displayed in the activity log.
 *
 * @param timestamp time when the activity occurred
 * @param level severity level assigned to the activity
 * @param message description displayed to the user
 */
public record ActivityLogEntry(
        LocalTime timestamp,
        ActivityLogLevel level,
        String message
) {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Creates and validates an activity log entry.
     */
    public ActivityLogEntry {
        Objects.requireNonNull(
                timestamp,
                "timestamp"
        );
        Objects.requireNonNull(
                level,
                "level"
        );
        message = requireMessage(message);
    }

    /**
     * Creates an activity entry using the current local time.
     *
     * @param level severity level assigned to the activity
     * @param message description displayed to the user
     * @return new activity log entry
     */
    public static ActivityLogEntry now(
            ActivityLogLevel level,
            String message
    ) {
        return new ActivityLogEntry(
                LocalTime.now(),
                level,
                message
        );
    }

    /**
     * Returns the timestamp formatted for display.
     *
     * @return timestamp using the {@code HH:mm:ss} format
     */
    public String formattedTime() {
        return timestamp.format(TIME_FORMATTER);
    }

    private static String requireMessage(String value) {
        Objects.requireNonNull(value, "message");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank"
            );
        }

        return value.trim();
    }

}
