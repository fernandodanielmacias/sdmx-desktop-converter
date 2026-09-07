package io.github.ordonovus.sdmxconverter.application.conversion.port;

import io.github.ordonovus.sdmxconverter.application.conversion.ConverterExecutionResult;
import io.github.ordonovus.sdmxconverter.application.conversion.SdmxConversionRequest;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Defines the contract for executing one SDMX conversion process.
 */
public interface SdmxConverterExecutor {

    /**
     * Executes one SDMX conversion.
     *
     * <p>The implementation may block the current thread and therefore
     * must not be invoked directly from the JavaFX Application Thread.</p>
     *
     * @param installation active validated Converter installation
     * @param request conversion request
     * @param outputListener listener notified for every process output line
     * @return technical process execution result
     * @throws IOException if the external process cannot be started or read
     * @throws InterruptedException if the executing thread is interrupted
     */
    ConverterExecutionResult execute(
            ConverterInstallation installation,
            SdmxConversionRequest request,
            Consumer<String> outputListener
    ) throws IOException, InterruptedException;

}