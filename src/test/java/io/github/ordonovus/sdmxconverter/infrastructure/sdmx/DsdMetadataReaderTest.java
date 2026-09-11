package io.github.ordonovus.sdmxconverter.infrastructure.sdmx;

import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies metadata extraction and secure parsing of SDMX Data Structure
 * Definition files.
 */
class DsdMetadataReaderTest {

    @TempDir
    Path temporaryDirectory;

    private final DsdMetadataReader metadataReader =
            new DsdMetadataReader();

    /**
     * Confirms that agency, ID and version are extracted from a namespaced
     * SDMX DataStructure element.
     *
     * @throws IOException if the temporary DSD cannot be written or read
     */
    @Test
    void shouldReadMetadataFromNamespacedDataStructure()
            throws IOException {
        Path dsdFile = writeDsd(
                "valid-structure.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <mes:Structure
                        xmlns:mes="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message"
                        xmlns:str="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure">
                    <mes:Structures>
                        <str:DataStructures>
                            <str:DataStructure
                                    agencyID=" ESTAT "
                                    id=" NA_MAIN "
                                    version=" 1.17.0 "/>
                        </str:DataStructures>
                    </mes:Structures>
                </mes:Structure>
                """
        );

        DsdMetadata metadata = metadataReader.read(dsdFile);

        assertEquals("ESTAT", metadata.agencyId());
        assertEquals("NA_MAIN", metadata.id());
        assertEquals("1.17.0", metadata.version());
        assertEquals(
                "ESTAT:NA_MAIN(1.17.0)",
                metadata.formattedIdentity()
        );
    }

    /**
     * Confirms that the first DataStructure is used when the document
     * contains more than one definition.
     *
     * @throws IOException if the temporary DSD cannot be written or read
     */
    @Test
    void shouldReadFirstDataStructure()
            throws IOException {
        Path dsdFile = writeDsd(
                "multiple-structures.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <str:Structures
                        xmlns:str="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure">
                    <str:DataStructure
                            agencyID="ESTAT"
                            id="FIRST"
                            version="1.0"/>
                    <str:DataStructure
                            agencyID="ESTAT"
                            id="SECOND"
                            version="2.0"/>
                </str:Structures>
                """
        );

        DsdMetadata metadata = metadataReader.read(dsdFile);

        assertEquals("FIRST", metadata.id());
        assertEquals("1.0", metadata.version());
    }

    /**
     * Confirms that an XML document without an SDMX DataStructure element is
     * rejected.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldRejectXmlWithoutDataStructure()
            throws IOException {
        Path dsdFile = writeDsd(
                "without-data-structure.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <str:Structures
                        xmlns:str="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure"/>
                """
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> metadataReader.read(dsdFile)
        );

        assertTrue(
                exception.getMessage().contains(
                        "does not contain an SDMX DataStructure"
                )
        );
    }

    /**
     * Confirms that a DataStructure with incomplete identity attributes is
     * rejected.
     *
     * @throws IOException if the temporary DSD cannot be written
     */
    @Test
    void shouldRejectDataStructureWithMissingIdentity()
            throws IOException {
        Path dsdFile = writeDsd(
                "incomplete-identity.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <str:DataStructure
                        xmlns:str="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure"
                        agencyID="ESTAT"
                        id="NA_MAIN"/>
                """
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> metadataReader.read(dsdFile)
        );

        assertTrue(
                exception.getMessage().contains(
                        "does not contain a valid agency, ID or version"
                )
        );
    }

    /**
     * Confirms that malformed XML cannot be interpreted as an SDMX
     * structure.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldRejectMalformedXml() throws IOException {
        Path dsdFile = writeDsd(
                "malformed.xml",
                """
                <str:DataStructure
                        xmlns:str="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure"
                        agencyID="ESTAT"
                        id="NA_MAIN"
                        version="1.17.0">
                """
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> metadataReader.read(dsdFile)
        );

        assertTrue(
                exception.getMessage().contains(
                        "could not be parsed as an SDMX structure file"
                )
        );
    }

    /**
     * Confirms that XML documents containing a document type declaration are
     * rejected to prevent external entity processing.
     *
     * @throws IOException if the temporary XML cannot be written
     */
    @Test
    void shouldRejectDocumentTypeDeclaration()
            throws IOException {
        Path dsdFile = writeDsd(
                "external-entity.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE Structure [
                    <!ENTITY external SYSTEM "file:///restricted-file">
                ]>
                <str:DataStructure
                        xmlns:str="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/structure"
                        agencyID="ESTAT"
                        id="NA_MAIN"
                        version="1.17.0">
                    &external;
                </str:DataStructure>
                """
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> metadataReader.read(dsdFile)
        );

        assertTrue(
                exception.getMessage().contains(
                        "could not be parsed as an SDMX structure file"
                )
        );
    }

    /**
     * Confirms that a nonexistent DSD file is rejected.
     */
    @Test
    void shouldRejectMissingDsdFile() {
        Path missingFile =
                temporaryDirectory.resolve("missing.xml");

        IOException exception = assertThrows(
                IOException.class,
                () -> metadataReader.read(missingFile)
        );

        assertTrue(
                exception.getMessage().contains(
                        "does not exist or is not a file"
                )
        );
    }

    /**
     * Writes an SDMX structure document to the temporary test directory.
     *
     * @param fileName temporary DSD file name
     * @param content XML document content
     * @return path of the created DSD file
     * @throws IOException if the file cannot be written
     */
    private Path writeDsd(
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