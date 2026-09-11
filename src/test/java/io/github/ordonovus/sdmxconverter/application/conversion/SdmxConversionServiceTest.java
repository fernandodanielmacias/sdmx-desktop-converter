package io.github.ordonovus.sdmxconverter.application.conversion;

import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxConverterExecutor;
import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxXmlOutputValidator;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterCompatibilityProfile;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies safe creation and replacement of SDMX-XML output files.
 */
class SdmxConversionServiceTest {

    private static final String STAGED_FILE_PREFIX =
            ".sdmx-converter-";

    private static final String GENERATED_XML =
            "<CompactData><Series><Obs/></Series></CompactData>";

    private static final DsdMetadata DSD_METADATA =
            new DsdMetadata(
                    "ESTAT",
                    "NA_MAIN",
                    "1.17.0"
            );

    @TempDir
    Path temporaryDirectory;

    private SdmxConversionRequestValidator requestValidator;
    private ConverterInstallation installation;
    private Path inputFile;
    private Path dsdFile;
    private Path headerFile;
    private Path outputFile;

    /**
     * Creates the valid baseline configuration required by each test.
     *
     * @throws IOException if a temporary file cannot be created
     */
    @BeforeEach
    void setUp() throws IOException {
        requestValidator =
                new SdmxConversionRequestValidator();

        inputFile = Files.writeString(
                temporaryDirectory.resolve("input.xlsx"),
                "test",
                StandardCharsets.UTF_8
        );

        dsdFile = Files.writeString(
                temporaryDirectory.resolve("structure.xml"),
                "<Structure/>",
                StandardCharsets.UTF_8
        );

        headerFile = Files.writeString(
                temporaryDirectory.resolve("header.prop"),
                "test=value",
                StandardCharsets.UTF_8
        );

        outputFile = temporaryDirectory.resolve("output.xml");

        installation = new ConverterInstallation(
                ConverterCompatibilityProfile.EUROSTAT_11_8_1,
                temporaryDirectory.resolve("converter"),
                temporaryDirectory.resolve("java.exe")
        );
    }

    /**
     * Confirms that a successful conversion promotes the staged XML to its
     * requested final destination.
     *
     * @throws Exception if the conversion workflow cannot be executed
     */
    @Test
    void shouldPublishValidatedXmlToFinalDestination()
            throws Exception {
        SdmxConversionService service = createSuccessfulService();

        SdmxConversionResult result = service.convert(
                installation,
                createRequest(ExistingOutputPolicy.REQUIRE_NEW),
                ignoredLine -> {
                }
        );

        assertTrue(result.isSuccessful());
        assertTrue(Files.exists(outputFile));
        assertEquals(
                GENERATED_XML,
                Files.readString(
                        outputFile,
                        StandardCharsets.UTF_8
                )
        );
        assertNoStagedFiles();
    }

    /**
     * Confirms that a validated replacement safely replaces the previous XML.
     *
     * @throws Exception if the conversion workflow cannot be executed
     */
    @Test
    void shouldReplaceExistingXmlAfterSuccessfulValidation()
            throws Exception {
        Files.writeString(
                outputFile,
                "previous-content",
                StandardCharsets.UTF_8
        );

        SdmxConversionService service = createSuccessfulService();

        SdmxConversionResult result = service.convert(
                installation,
                createRequest(
                        ExistingOutputPolicy.REPLACE_EXISTING
                ),
                ignoredLine -> {
                }
        );

        assertTrue(result.isSuccessful());
        assertEquals(
                GENERATED_XML,
                Files.readString(
                        outputFile,
                        StandardCharsets.UTF_8
                )
        );
        assertNoStagedFiles();
    }

    /**
     * Confirms that an existing XML remains unchanged when validation of the
     * newly generated XML fails.
     *
     * @throws Exception if the conversion workflow cannot be executed
     */
    @Test
    void shouldPreserveExistingXmlWhenValidationFails()
            throws Exception {
        String previousContent = "previous-content";

        Files.writeString(
                outputFile,
                previousContent,
                StandardCharsets.UTF_8
        );

        SdmxConversionService service =
                createInvalidValidationService(
                        "El XML generado no contiene elementos Series."
                );

        SdmxConversionResult result = service.convert(
                installation,
                createRequest(
                        ExistingOutputPolicy.REPLACE_EXISTING
                ),
                ignoredLine -> {
                }
        );

        assertFalse(result.isSuccessful());
        assertEquals(
                previousContent,
                Files.readString(
                        outputFile,
                        StandardCharsets.UTF_8
                )
        );
        assertNoStagedFiles();
    }

    /**
     * Confirms that no final XML is created when validation fails.
     *
     * @throws Exception if the conversion workflow cannot be executed
     */
    @Test
    void shouldNotCreateFinalXmlWhenValidationFails()
            throws Exception {
        SdmxConversionService service =
                createInvalidValidationService(
                        "El XML generado no contiene observaciones."
                );

        SdmxConversionResult result = service.convert(
                installation,
                createRequest(ExistingOutputPolicy.REQUIRE_NEW),
                ignoredLine -> {
                }
        );

        assertFalse(result.isSuccessful());
        assertFalse(Files.exists(outputFile));
        assertNoStagedFiles();
    }

    /**
     * Confirms that temporary output is removed when the Converter execution
     * throws an exception.
     *
     * @throws IOException if a temporary file operation fails
     */
    @Test
    void shouldDeleteStagedXmlWhenExecutionFails()
            throws IOException {
        SdmxConverterExecutor failingExecutor =
                (activeInstallation, request, outputListener) -> {
                    Files.writeString(
                            request.outputFile(),
                            "incomplete-content",
                            StandardCharsets.UTF_8
                    );

                    throw new IOException(
                            "Simulated Converter failure"
                    );
                };

        SdmxConversionService service =
                new SdmxConversionService(
                        requestValidator,
                        failingExecutor,
                        this::createValidValidationResult
                );
        assertThrows(
                IOException.class,
                () -> service.convert(
                        installation,
                        createRequest(
                                ExistingOutputPolicy.REQUIRE_NEW
                        ),
                        ignoredLine -> {
                        }
                )
        );

        assertFalse(Files.exists(outputFile));
        assertNoStagedFiles();
    }

    private SdmxConversionService createSuccessfulService() {
        return new SdmxConversionService(
                requestValidator,
                createSuccessfulExecutor(),
                this::createValidValidationResult
        );
    }

    private SdmxConversionService createInvalidValidationService(
            String validationError
    ) {
        SdmxXmlOutputValidator invalidValidator =
                xmlFile -> new SdmxXmlValidationResult(
                        xmlFile,
                        generatedXmlSize(),
                        0,
                        0,
                        List.of(validationError)
                );

        return new SdmxConversionService(
                requestValidator,
                createSuccessfulExecutor(),
                invalidValidator
        );
    }

    private long generatedXmlSize() {
        return GENERATED_XML.getBytes(
                StandardCharsets.UTF_8
        ).length;
    }

    private SdmxConverterExecutor createSuccessfulExecutor() {
        return (activeInstallation, request, outputListener) -> {
            Files.writeString(
                    request.outputFile(),
                    GENERATED_XML,
                    StandardCharsets.UTF_8
            );

            outputListener.accept(
                    "Simulated successful conversion"
            );

            Instant startedAt = Instant.now();
            Instant finishedAt = startedAt.plusMillis(100);

            return new ConverterExecutionResult(
                    0,
                    startedAt,
                    finishedAt,
                    List.of(
                            "Simulated successful conversion"
                    )
            );
        };
    }

    private SdmxXmlValidationResult createValidValidationResult(
            Path xmlFile
    ) {
        return new SdmxXmlValidationResult(
                xmlFile,
                generatedXmlSize(),
                1,
                1,
                List.of()
        );
    }

    private SdmxConversionRequest createRequest(
            ExistingOutputPolicy existingOutputPolicy
    ) {
        return new SdmxConversionRequest(
                inputFile,
                outputFile,
                dsdFile,
                headerFile,
                DSD_METADATA,
                SdmxConversionParameters.defaults(),
                existingOutputPolicy
        );
    }

    private void assertNoStagedFiles() throws IOException {
        try (var files = Files.list(temporaryDirectory)) {
            boolean stagedFileExists = files
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .anyMatch(fileName ->
                            fileName.startsWith(
                                    STAGED_FILE_PREFIX
                            )
                    );

            assertFalse(
                    stagedFileExists,
                    "No staged output file should remain"
            );
        }
    }
}