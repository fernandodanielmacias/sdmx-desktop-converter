package io.github.ordonovus.sdmxconverter.application.conversion;

import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the validation rules applied before executing an SDMX conversion.
 */
class SdmxConversionRequestValidatorTest {

    private static final DsdMetadata DSD_METADATA =
            new DsdMetadata(
                    "ESTAT",
                    "NA_MAIN",
                    "1.17.0"
            );

    @TempDir
    Path temporaryDirectory;

    private SdmxConversionRequestValidator validator;
    private Path inputFile;
    private Path dsdFile;
    private Path headerFile;
    private Path outputFile;

    /**
     * Creates the valid baseline files required by each test.
     *
     * @throws IOException if a temporary file cannot be created
     */
    @BeforeEach
    void setUp() throws IOException {
        validator = new SdmxConversionRequestValidator();

        inputFile = Files.createFile(
                temporaryDirectory.resolve("input.xlsx")
        );

        dsdFile = Files.createFile(
                temporaryDirectory.resolve("structure.xml")
        );

        headerFile = Files.createFile(
                temporaryDirectory.resolve("header.prop")
        );

        outputFile = temporaryDirectory.resolve("output.xml");
    }

    /**
     * Confirms that a complete request with a new destination is valid.
     */
    @Test
    void shouldAcceptValidRequestWithNewOutputFile() {
        SdmxConversionRequest request = createRequest(
                ExistingOutputPolicy.REQUIRE_NEW
        );

        ConversionRequestValidationResult result =
                validator.validate(request);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    /**
     * Confirms that an existing output is rejected when a new file is
     * required.
     *
     * @throws IOException if the existing output cannot be created
     */
    @Test
    void shouldRejectExistingOutputWhenNewFileIsRequired()
            throws IOException {
        Files.createFile(outputFile);

        SdmxConversionRequest request = createRequest(
                ExistingOutputPolicy.REQUIRE_NEW
        );

        ConversionRequestValidationResult result =
                validator.validate(request);

        assertFalse(result.isValid());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "El archivo XML de salida ya existe"
                        ))
        );
    }

    /**
     * Confirms that a writable existing output is accepted when replacement
     * is authorized.
     *
     * @throws IOException if the existing output cannot be created
     */
    @Test
    void shouldAcceptExistingOutputWhenReplacementIsAuthorized()
            throws IOException {
        Files.createFile(outputFile);

        SdmxConversionRequest request = createRequest(
                ExistingOutputPolicy.REPLACE_EXISTING
        );

        ConversionRequestValidationResult result =
                validator.validate(request);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    /**
     * Confirms that an output path pointing to a directory is rejected even
     * when replacement is authorized.
     *
     * @throws IOException if the output directory cannot be created
     */
    @Test
    void shouldRejectOutputPathThatIsNotARegularFile()
            throws IOException {
        Files.createDirectory(outputFile);

        SdmxConversionRequest request = createRequest(
                ExistingOutputPolicy.REPLACE_EXISTING
        );

        ConversionRequestValidationResult result =
                validator.validate(request);

        assertFalse(result.isValid());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                "no corresponde a un archivo"
                        ))
        );
    }

    /**
     * Confirms that unsupported Excel extensions are rejected.
     *
     * @throws IOException if the temporary input file cannot be created
     */
    @Test
    void shouldRejectUnsupportedInputExtension()
            throws IOException {
        Path unsupportedInput = Files.createFile(
                temporaryDirectory.resolve("input.csv")
        );

        SdmxConversionRequest request =
                new SdmxConversionRequest(
                        unsupportedInput,
                        outputFile,
                        dsdFile,
                        headerFile,
                        DSD_METADATA,
                        SdmxConversionParameters.defaults(),
                        ExistingOutputPolicy.REQUIRE_NEW
                );

        ConversionRequestValidationResult result =
                validator.validate(request);

        assertFalse(result.isValid());
        assertTrue(
                result.errors().stream()
                        .anyMatch(error -> error.contains(
                                ".xls o .xlsx"
                        ))
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
}
