package io.github.ordonovus.sdmxconverter.infrastructure.converter;

import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionParameters;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;
import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the command used to execute the Eurostat SDMX Converter CLI.
 *
 * <p>The generated command is equivalent to the validated command from
 * {@code converter-cli.bat}, but it uses an explicit private Java runtime
 * and does not depend on {@code PATH}, {@code JAVA_HOME}, or a command
 * shell.</p>
 */
public final class SdmxConverterCommandFactory {

    private static final String MAXIMUM_HEAP_ARGUMENT = "-Xmx1024m";
    private static final String CLASSPATH_ARGUMENT = "-classpath";

    /**
     * Builds the complete Converter CLI command.
     *
     * @param installation active validated Converter installation
     * @param request conversion request
     * @return immutable command and argument collection
     */
    public List<String> create(
            ConverterInstallation installation,
            SdmxConversionRequest request
    ) {
        Objects.requireNonNull(installation, "installation");
        Objects.requireNonNull(request, "request");

        List<String> command = new ArrayList<>();

        command.add(
                installation.javaExecutable().toString()
        );
        command.add(MAXIMUM_HEAP_ARGUMENT);
        command.add(CLASSPATH_ARGUMENT);
        command.add(createClasspath(installation));
        command.add(
                installation.profile().mainClass()
        );

        addConversionArguments(command, request);

        return List.copyOf(command);
    }

    private String createClasspath(
            ConverterInstallation installation
    ) {
        String configurationWildcard =
                installation.workingDirectory()
                        .resolve("config")
                        + File.separator
                        + "*";

        return installation.launcherJar()
                + File.pathSeparator
                + configurationWildcard;
    }

    private void addConversionArguments(
            List<String> command,
            SdmxConversionRequest request
    ) {
        SdmxConversionParameters parameters =
                request.parameters();

        DsdMetadata metadata = request.dsdMetadata();

        addOption(
                command,
                "-validation",
                parameters.validation()
        );

        addOption(
                command,
                "-inputFile",
                request.inputFile()
        );

        addOption(
                command,
                "-outputFile",
                request.outputFile()
        );

        addOption(
                command,
                "-dsd_file",
                request.dsdFile()
        );

        addOption(
                command,
                "-dsd_agency",
                metadata.agencyId()
        );

        addOption(
                command,
                "-dsd_id",
                metadata.id()
        );

        addOption(
                command,
                "-dsd_version",
                metadata.version()
        );

        addOption(
                command,
                "-header_file",
                request.headerFile()
        );

        addOption(
                command,
                "-from",
                parameters.inputFormat()
        );

        addOption(
                command,
                "-to",
                parameters.outputFormat()
        );

        addOption(
                command,
                "-maxErrorNumber",
                parameters.maximumErrorCount()
        );

        addOption(
                command,
                "-reg",
                parameters.useRegistry()
        );

        addOption(
                command,
                "-errorIfEmpty",
                parameters.errorIfEmpty()
        );

        addOption(
                command,
                "-groupedOutput",
                parameters.groupedOutput()
        );
    }

    private void addOption(
            List<String> command,
            String option,
            Object value
    ) {
        command.add(option);
        command.add(String.valueOf(value));
    }

}