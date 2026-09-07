package io.github.ordonovus.sdmxconverter.application.conversion;

import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxConverterExecutor;
import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxXmlOutputValidator;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Coordinates the complete workflow for one Excel to SDMX-XML conversion.
 *
 * <p>The service validates the request, executes the external Converter,
 * validates the generated XML and returns the complete conversion result.</p>
 */
public final class SdmxConversionService {

    private final SdmxConversionRequestValidator requestValidator;
    private final SdmxConverterExecutor converterExecutor;
    private final SdmxXmlOutputValidator xmlOutputValidator;

    /**
     * Creates the conversion service with its required collaborators.
     *
     * @param requestValidator validator executed before starting the process
     * @param converterExecutor external Converter executor
     * @param xmlOutputValidator generated XML validator
     */
    public SdmxConversionService(
            SdmxConversionRequestValidator requestValidator,
            SdmxConverterExecutor converterExecutor,
            SdmxXmlOutputValidator xmlOutputValidator
    ) {
        this.requestValidator = Objects.requireNonNull(
                requestValidator,
                "requestValidator"
        );

        this.converterExecutor = Objects.requireNonNull(
                converterExecutor,
                "converterExecutor"
        );

        this.xmlOutputValidator = Objects.requireNonNull(
                xmlOutputValidator,
                "xmlOutputValidator"
        );
    }

    /**
     * Performs one complete SDMX conversion.
     *
     * <p>This method is blocking and must be invoked from a background
     * thread.</p>
     *
     * @param installation active validated Converter installation
     * @param request conversion request
     * @param outputListener listener notified for each Converter output line
     * @return complete conversion result
     * @throws InvalidConversionRequestException if the request is invalid
     * @throws IOException if the Converter process cannot be started or read
     * @throws InterruptedException if the executing thread is interrupted
     */
    public SdmxConversionResult convert(
            ConverterInstallation installation,
            SdmxConversionRequest request,
            Consumer<String> outputListener
    ) throws InvalidConversionRequestException,
            IOException,
            InterruptedException {
        Objects.requireNonNull(installation, "installation");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(
                outputListener,
                "outputListener"
        );

        ConversionRequestValidationResult validationResult =
                requestValidator.validate(request);

        if (!validationResult.isValid()) {
            throw new InvalidConversionRequestException(
                    validationResult
            );
        }

        ConverterExecutionResult executionResult =
                converterExecutor.execute(
                        installation,
                        request,
                        outputListener
                );

        SdmxXmlValidationResult xmlValidationResult =
                xmlOutputValidator.validate(
                        request.outputFile()
                );

        return new SdmxConversionResult(
                request,
                executionResult,
                xmlValidationResult
        );
    }

}