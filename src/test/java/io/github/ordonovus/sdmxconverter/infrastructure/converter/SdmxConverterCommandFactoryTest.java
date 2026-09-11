package io.github.ordonovus.sdmxconverter.infrastructure.converter;

import io.github.ordonovus.sdmxconverter.application.conversion.ExistingOutputPolicy;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionParameters;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterCompatibilityProfile;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the operating-system command generated for the Eurostat SDMX
 * Converter CLI.
 */
class SdmxConverterCommandFactoryTest {

    @TempDir
    Path temporaryDirectory;

    private SdmxConverterCommandFactory commandFactory;
    private ConverterInstallation installation;
    private SdmxConversionRequest request;

    /**
     * Creates the converter installation and conversion request used by each
     * command-generation test.
     */
    @BeforeEach
    void setUp() {
        commandFactory =
                new SdmxConverterCommandFactory();

        Path installationDirectory =
                temporaryDirectory.resolve("ConverterCLIApp");

        Path javaExecutable =
                temporaryDirectory
                        .resolve("runtime")
                        .resolve("bin")
                        .resolve("java.exe");

        installation = new ConverterInstallation(
                ConverterCompatibilityProfile.EUROSTAT_11_8_1,
                installationDirectory,
                javaExecutable
        );

        request = new SdmxConversionRequest(
                temporaryDirectory.resolve("input file.xlsx"),
                temporaryDirectory.resolve("output file.xml"),
                temporaryDirectory.resolve("structure.xml"),
                temporaryDirectory.resolve("header.prop"),
                new DsdMetadata(
                        "ESTAT",
                        "NA_MAIN",
                        "1.17.0"
                ),
                SdmxConversionParameters.defaults(),
                ExistingOutputPolicy.REQUIRE_NEW
        );
    }

    /**
     * Confirms that the generated command contains the private Java runtime,
     * classpath, main class and complete set of conversion arguments.
     */
    @Test
    void shouldCreateCompleteConverterCommand() {
        List<String> command = commandFactory.create(
                installation,
                request
        );

        String expectedClasspath =
                installation.launcherJar()
                        + File.pathSeparator
                        + installation.workingDirectory()
                        .resolve("config")
                        + File.separator
                        + "*";

        List<String> expectedCommand = List.of(
                installation.javaExecutable().toString(),
                "-Xmx1024m",
                "-classpath",
                expectedClasspath,
                installation.profile().mainClass(),
                "-validation",
                "true",
                "-inputFile",
                request.inputFile().toString(),
                "-outputFile",
                request.outputFile().toString(),
                "-dsd_file",
                request.dsdFile().toString(),
                "-dsd_agency",
                "ESTAT",
                "-dsd_id",
                "NA_MAIN",
                "-dsd_version",
                "1.17.0",
                "-header_file",
                request.headerFile().toString(),
                "-from",
                "EXCEL",
                "-to",
                "COMPACT_SDMX",
                "-maxErrorNumber",
                "100",
                "-reg",
                "false",
                "-errorIfEmpty",
                "true",
                "-groupedOutput",
                "true"
        );

        assertEquals(expectedCommand, command);
    }

    /**
     * Confirms that the generated command cannot be modified by callers.
     */
    @Test
    void shouldReturnImmutableCommand() {
        List<String> command = commandFactory.create(
                installation,
                request
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> command.add("unexpected-argument")
        );
    }
}