package io.github.ordonovus.sdmxconverter.application.conversion;

/**
 * Defines how a conversion request handles an existing destination XML file.
 */
public enum ExistingOutputPolicy {

    /**
     * Requires the destination XML file not to exist before conversion.
     */
    REQUIRE_NEW,

    /**
     * Replaces the existing destination only after the newly generated XML
     * has completed conversion and validation successfully.
     */
    REPLACE_EXISTING
}