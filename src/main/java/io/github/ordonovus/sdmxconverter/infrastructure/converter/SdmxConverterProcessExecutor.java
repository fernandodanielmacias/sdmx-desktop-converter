package io.github.ordonovus.sdmxconverter.infrastructure.converter;

import io.github.ordonovus.sdmxconverter.application.conversion.ConverterExecutionResult;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxConverterExecutor;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Executes the Eurostat SDMX Converter as an independent operating-system
 * process.
 *
 * <p>The process uses the private Java runtime from the active converter
 * installation and captures its standard and error output without opening
 * a command-line window.</p>
 */
public final class SdmxConverterProcessExecutor
        implements SdmxConverterExecutor {

    private final SdmxConverterCommandFactory commandFactory;

    /**
     * Creates an executor with the default command factory.
     */
    public SdmxConverterProcessExecutor() {
        this(new SdmxConverterCommandFactory());
    }

    /**
     * Creates an executor with the provided command factory.
     *
     * @param commandFactory factory used to construct the process command
     */
    public SdmxConverterProcessExecutor(
            SdmxConverterCommandFactory commandFactory
    ) {
        this.commandFactory = Objects.requireNonNull(
                commandFactory,
                "commandFactory"
        );
    }

    /**
     * Executes one conversion and reports every captured output line.
     *
     * <p>This method is blocking and must not be invoked directly from the
     * JavaFX Application Thread.</p>
     *
     * @param installation active validated Converter installation
     * @param request conversion request
     * @param outputListener listener notified for each process output line
     * @return technical process execution result
     * @throws IOException if the process cannot be started or read
     * @throws InterruptedException if the executing thread is interrupted
     */
    @Override
    public ConverterExecutionResult execute(
            ConverterInstallation installation,
            SdmxConversionRequest request,
            Consumer<String> outputListener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(installation, "installation");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(
                outputListener,
                "outputListener"
        );

        List<String> command = commandFactory.create(
                installation,
                request
        );

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.directory(
                installation.workingDirectory().toFile()
        );

        processBuilder.redirectErrorStream(true);

        Instant startedAt = Instant.now();
        Process process = processBuilder.start();

        List<String> outputLines = new ArrayList<>();

        try {
            captureOutput(
                    process,
                    outputLines,
                    outputListener
            );

            int exitCode = process.waitFor();
            Instant finishedAt = Instant.now();

            return new ConverterExecutionResult(
                    exitCode,
                    startedAt,
                    finishedAt,
                    outputLines
            );
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw exception;
        } catch (IOException | RuntimeException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    private void captureOutput(
            Process process,
            List<String> outputLines,
            Consumer<String> outputListener
    ) throws IOException {
        Charset processCharset = Charset.defaultCharset();

        try (var reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        processCharset
                )
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
                outputListener.accept(line);
            }
        }
    }

}