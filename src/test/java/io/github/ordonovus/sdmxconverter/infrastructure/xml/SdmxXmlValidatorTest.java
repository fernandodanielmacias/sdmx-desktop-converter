package io.github.ordonovus.sdmxconverter.infrastructure.xml;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxXmlValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the basic integrity validation applied to generated SDMX-XML
 * files.
 */
class SdmxXmlValidatorTest {

    @TempDir
    Path temporaryDirectory;

    private final SdmxXmlValidator validator =
            new SdmxXmlValidator();

    /**
     * Confirms that valid namespaced XML is accepted and its Series and Obs
     * elements are counted by local name.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldValidateAndCountNamespacedSdmxElements()
            throws IOException {
        Path xmlFile = writeXml(
                "valid.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <mes:CompactData
                        xmlns:mes="urn:sdmx:org.sdmx.infomodel.message"
                        xmlns:gen="urn:sdmx:org.sdmx.infomodel.datastructure">
                    <gen:Series>
                        <gen:Obs/>
                        <gen:Obs/>
                    </gen:Series>
                    <gen:Series>
                        <gen:Obs/>
                    </gen:Series>
                </mes:CompactData>
                """
        );

        SdmxXmlValidationResult result =
                validator.validate(xmlFile);

        assertTrue(result.isValid());
        assertEquals(2, result.seriesCount());
        assertEquals(3, result.observationCount());
        assertEquals(Files.size(xmlFile), result.fileSize());
        assertEquals(
                xmlFile.toAbsolutePath().normalize(),
                result.xmlFile()
        );
        assertTrue(result.errors().isEmpty());
    }

    /**
     * Confirms that a missing XML file produces an invalid result.
     */
    @Test
    void shouldRejectMissingXmlFile() {
        Path missingFile =
                temporaryDirectory.resolve("missing.xml");

        SdmxXmlValidationResult result =
                validator.validate(missingFile);

        assertFalse(result.isValid());
        assertEquals(0, result.fileSize());
        assertEquals(0, result.seriesCount());
        assertEquals(0, result.observationCount());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "no existe"
                        ))
        );
    }

    /**
     * Confirms that an empty XML file produces an invalid result.
     *
     * @throws IOException if the temporary file cannot be created
     */
    @Test
    void shouldRejectEmptyXmlFile() throws IOException {
        Path xmlFile = Files.createFile(
                temporaryDirectory.resolve("empty.xml")
        );

        SdmxXmlValidationResult result =
                validator.validate(xmlFile);

        assertFalse(result.isValid());
        assertEquals(0, result.fileSize());
        assertEquals(0, result.seriesCount());
        assertEquals(0, result.observationCount());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "está vacío"
                        ))
        );
    }

    /**
     * Confirms that malformed XML produces an invalid result.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldRejectMalformedXml() throws IOException {
        Path xmlFile = writeXml(
                "malformed.xml",
                "<CompactData><Series><Obs/></CompactData>"
        );

        SdmxXmlValidationResult result =
                validator.validate(xmlFile);

        assertFalse(result.isValid());
        assertEquals(0, result.seriesCount());
        assertEquals(0, result.observationCount());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "no es un XML válido"
                        ))
        );
    }

    /**
     * Confirms that XML without Series elements produces an invalid result
     * while preserving its observation count.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldRejectXmlWithoutSeriesElements()
            throws IOException {
        Path xmlFile = writeXml(
                "without-series.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <CompactData>
                    <Obs/>
                    <Obs/>
                </CompactData>
                """
        );

        SdmxXmlValidationResult result =
                validator.validate(xmlFile);

        assertFalse(result.isValid());
        assertEquals(0, result.seriesCount());
        assertEquals(2, result.observationCount());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "no contiene elementos Series"
                        ))
        );
    }

    /**
     * Confirms that XML without Obs elements produces an invalid result while
     * preserving its series count.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldRejectXmlWithoutObservationElements()
            throws IOException {
        Path xmlFile = writeXml(
                "without-observations.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <CompactData>
                    <Series/>
                    <Series/>
                </CompactData>
                """
        );

        SdmxXmlValidationResult result =
                validator.validate(xmlFile);

        assertFalse(result.isValid());
        assertEquals(2, result.seriesCount());
        assertEquals(0, result.observationCount());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "no contiene elementos Obs"
                        ))
        );
    }

    /**
     * Writes an XML document to the temporary test directory.
     *
     * @param fileName temporary XML file name
     * @param content XML document content
     * @return path of the created XML file
     * @throws IOException if the file cannot be written
     */
    private Path writeXml(
            String fileName,
            String content
    ) throws IOException {
        return Files.writeString(
                temporaryDirectory.resolve(fileName),
                content,
                StandardCharsets.UTF_8
        );
    }
}
