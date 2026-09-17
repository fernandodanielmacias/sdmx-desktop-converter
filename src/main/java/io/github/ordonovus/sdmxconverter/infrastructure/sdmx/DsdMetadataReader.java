package io.github.ordonovus.sdmxconverter.infrastructure.sdmx;

import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Reads identity metadata from an SDMX Data Structure Definition file.
 */
public class DsdMetadataReader {

    private static final String SDMX_STRUCTURE_NAMESPACE =
            "http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure";

    /**
     * Reads the first SDMX {@code DataStructure} element found in the
     * supplied file.
     *
     * @param dsdFile path to the SDMX structure file
     * @return metadata containing the DSD agency, ID and version
     * @throws IOException if the file cannot be read or does not contain
     *                     valid DSD identity information
     */
    public DsdMetadata read(Path dsdFile) throws IOException {
        validateFile(dsdFile);

        try {
            Document document = createDocumentBuilder()
                    .parse(dsdFile.toFile());

            NodeList dataStructures = document.getElementsByTagNameNS(
                    SDMX_STRUCTURE_NAMESPACE,
                    "DataStructure"
            );

            if (dataStructures.getLength() == 0) {
                throw new IOException(
                        "The selected XML file does not contain an SDMX "
                                + "DataStructure element."
                );
            }

            Element dataStructure = (Element) dataStructures.item(0);

            return new DsdMetadata(
                    dataStructure.getAttribute("agencyID"),
                    dataStructure.getAttribute("id"),
                    dataStructure.getAttribute("version")
            );
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException(
                    "The selected file could not be parsed as an SDMX "
                            + "structure file.",
                    exception
            );
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "The SDMX DataStructure does not contain a valid agency, "
                            + "ID or version.",
                    exception
            );
        }
    }

    /**
     * Creates a securely configured document builder that reports parsing
     * failures through exceptions without writing diagnostics to the console.
     *
     * @return configured document builder
     * @throws ParserConfigurationException if secure parser configuration fails
     */
    private DocumentBuilder createDocumentBuilder()
            throws ParserConfigurationException {
        DocumentBuilder documentBuilder =
                createDocumentBuilderFactory()
                        .newDocumentBuilder();

        documentBuilder.setErrorHandler(
                new StrictXmlErrorHandler()
        );

        return documentBuilder;
    }

    private DocumentBuilderFactory createDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );
        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        return factory;
    }

    private void validateFile(Path dsdFile) throws IOException {
        Objects.requireNonNull(
                dsdFile,
                "The DSD file path must not be null"
        );

        if (!Files.isRegularFile(dsdFile)) {
            throw new IOException(
                    "The selected DSD file does not exist or is not a file."
            );
        }

        if (!Files.isReadable(dsdFile)) {
            throw new IOException(
                    "The selected DSD file cannot be read."
            );
        }
    }

    /**
     * Propagates every XML parser diagnostic without printing it to the console.
     */
    private static final class StrictXmlErrorHandler
            implements ErrorHandler {

        @Override
        public void warning(
                SAXParseException exception
        ) throws SAXException {
            throw exception;
        }

        @Override
        public void error(
                SAXParseException exception
        ) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(
                SAXParseException exception
        ) throws SAXException {
            throw exception;
        }
    }

}
