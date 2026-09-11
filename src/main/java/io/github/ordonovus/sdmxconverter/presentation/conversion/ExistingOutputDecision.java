package io.github.ordonovus.sdmxconverter.presentation.conversion;

/**
 * Represents the action selected by the user when destination XML files
 * already exist.
 */
public enum ExistingOutputDecision {

    /**
     * Replaces existing XML files only after their new versions pass
     * conversion and validation successfully.
     */
    REPLACE,

    /**
     * Excludes requests whose destination XML files already exist.
     */
    SKIP,

    /**
     * Cancels the complete batch before any conversion starts.
     */
    CANCEL
}