package io.github.ordonovus.sdmxconverter.application.conversion.port;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxXmlValidationResult;

import java.nio.file.Path;

/**
 * Defines the contract for validating a generated SDMX-XML file.
 */
public interface SdmxXmlOutputValidator {

    /**
     * Validates the generated XML file and obtains its SDMX element counts.
     *
     * @param xmlFile generated XML file
     * @return XML validation result
     */
    SdmxXmlValidationResult validate(Path xmlFile);

}