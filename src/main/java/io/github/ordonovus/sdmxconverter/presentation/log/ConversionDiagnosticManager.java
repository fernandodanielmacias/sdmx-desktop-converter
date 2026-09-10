package io.github.ordonovus.sdmxconverter.presentation.log;

import io.github.ordonovus.sdmxconverter.presentation.conversion.ConversionBatchDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Preserves conversion batch diagnostics generated during the current
 * application session.
 *
 * <p>The stored diagnostics can later be included when the user exports the
 * activity log. Clearing this manager does not delete any previously exported
 * log files.</p>
 */
public final class ConversionDiagnosticManager {

    private final List<ConversionBatchDiagnostic> diagnostics =
            new ArrayList<>();

    /**
     * Adds a completed batch diagnostic to the current session.
     *
     * @param diagnostic batch diagnostic to preserve
     */
    public void add(
            ConversionBatchDiagnostic diagnostic
    ) {
        Objects.requireNonNull(diagnostic, "diagnostic");

        synchronized (diagnostics) {
            diagnostics.add(diagnostic);
        }
    }

    /**
     * Returns an immutable snapshot of all stored batch diagnostics.
     *
     * @return immutable diagnostic collection
     */
    public List<ConversionBatchDiagnostic> snapshot() {
        synchronized (diagnostics) {
            return List.copyOf(diagnostics);
        }
    }

    /**
     * Indicates whether the current session contains batch diagnostics.
     *
     * @return {@code true} when no diagnostics have been stored
     */
    public boolean isEmpty() {
        synchronized (diagnostics) {
            return diagnostics.isEmpty();
        }
    }

    /**
     * Removes every batch diagnostic stored in the current session.
     */
    public void clear() {
        synchronized (diagnostics) {
            diagnostics.clear();
        }
    }

}