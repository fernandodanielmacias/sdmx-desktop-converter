package io.github.ordonovus.sdmxconverter.infrastructure.xml;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxXmlValidationResult;
import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxXmlOutputValidator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Performs basic integrity validation of a generated SDMX-XML file.
 *
 * <p>The document is processed as a stream to avoid loading the complete
 * XML into memory. Element prefixes are ignored and elements are identified
 * through their local names.</p>
 */
public final class SdmxXmlValidator
        implements SdmxXmlOutputValidator {

    private static final String SERIES_ELEMENT = "Series";
    private static final String OBSERVATION_ELEMENT = "Obs";

    /**
     * Validates a generated SDMX-XML file and counts its series and
     * observations.
     *
     * @param xmlFile generated XML file
     * @return XML validation result
     */
    @Override
    public SdmxXmlValidationResult validate(Path xmlFile) {
        Path normalizedFile = Objects.requireNonNull(
                xmlFile,
                "xmlFile"
        ).toAbsolutePath().normalize();

        List<String> errors = new ArrayList<>();

        if (!validateFile(normalizedFile, errors)) {
            return createInvalidResult(
                    normalizedFile,
                    errors
            );
        }

        long fileSize;

        try {
            fileSize = Files.size(normalizedFile);
        } catch (IOException exception) {
            errors.add(
                    "No fue posible determinar el tamaño del XML: "
                            + exception.getMessage()
            );

            return createInvalidResult(
                    normalizedFile,
                    errors
            );
        }

        if (fileSize == 0) {
            errors.add(
                    "El archivo XML generado está vacío."
            );

            return new SdmxXmlValidationResult(
                    normalizedFile,
                    0,
                    0,
                    0,
                    errors
            );
        }

        ParsingCounts counts;

        try {
            counts = parseDocument(normalizedFile);
        } catch (IOException | XMLStreamException exception) {
            errors.add(
                    "El archivo generado no es un XML válido: "
                            + requireExceptionMessage(exception)
            );

            return new SdmxXmlValidationResult(
                    normalizedFile,
                    fileSize,
                    0,
                    0,
                    errors
            );
        }

        if (counts.seriesCount() == 0) {
            errors.add(
                    "El XML generado no contiene elementos Series."
            );
        }

        if (counts.observationCount() == 0) {
            errors.add(
                    "El XML generado no contiene elementos Obs."
            );
        }

        return new SdmxXmlValidationResult(
                normalizedFile,
                fileSize,
                counts.seriesCount(),
                counts.observationCount(),
                errors
        );
    }

    private boolean validateFile(
            Path xmlFile,
            List<String> errors
    ) {
        if (!Files.exists(xmlFile)) {
            errors.add(
                    "El archivo XML generado no existe: "
                            + xmlFile
            );
            return false;
        }

        if (!Files.isRegularFile(xmlFile)) {
            errors.add(
                    "La ruta del resultado no corresponde "
                            + "a un archivo: "
                            + xmlFile
            );
            return false;
        }

        if (!Files.isReadable(xmlFile)) {
            errors.add(
                    "El archivo XML generado no se puede leer: "
                            + xmlFile
            );
            return false;
        }

        return true;
    }

    private ParsingCounts parseDocument(
            Path xmlFile
    ) throws IOException, XMLStreamException {
        XMLInputFactory factory =
                XMLInputFactory.newFactory();

        configureSecureProcessing(factory);

        long seriesCount = 0;
        long observationCount = 0;

        try (InputStream inputStream =
                     Files.newInputStream(xmlFile)) {

            XMLStreamReader reader =
                    factory.createXMLStreamReader(inputStream);

            try {
                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event != XMLStreamConstants.START_ELEMENT) {
                        continue;
                    }

                    String localName = reader.getLocalName();

                    if (SERIES_ELEMENT.equals(localName)) {
                        seriesCount++;
                    } else if (OBSERVATION_ELEMENT.equals(localName)) {
                        observationCount++;
                    }
                }
            } finally {
                reader.close();
            }
        }

        return new ParsingCounts(
                seriesCount,
                observationCount
        );
    }

    private void configureSecureProcessing(
            XMLInputFactory factory
    ) {
        factory.setProperty(
                XMLInputFactory.SUPPORT_DTD,
                false
        );

        factory.setProperty(
                XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                false
        );

        factory.setXMLResolver(
                (publicId, systemId, baseUri, namespace) -> {
                    throw new XMLStreamException(
                            "External XML resources are disabled"
                    );
                }
        );
    }

    private SdmxXmlValidationResult createInvalidResult(
            Path xmlFile,
            List<String> errors
    ) {
        return new SdmxXmlValidationResult(
                xmlFile,
                0,
                0,
                0,
                errors
        );
    }

    private String requireExceptionMessage(
            Exception exception
    ) {
        String message = exception.getMessage();

        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private record ParsingCounts(
            long seriesCount,
            long observationCount
    ) {
    }

}