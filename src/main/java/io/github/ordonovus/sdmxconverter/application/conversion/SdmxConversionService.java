package io.github.ordonovus.sdmxconverter.application.conversion;

import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxConverterExecutor;
import io.github.ordonovus.sdmxconverter.application.conversion.port.SdmxXmlOutputValidator;
import io.github.ordonovus.sdmxconverter.application.converter.installation.ConverterInstallation;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Coordinates the complete workflow for one Excel to SDMX-XML conversion.
 *
 * <p>The service validates the request, executes the external Converter using
 * a temporary output file, validates the generated XML and promotes it to the
 * final destination only after a successful result.</p>
 */
public final class SdmxConversionService {

    private static final String STAGED_FILE_PREFIX =
            ".sdmx-converter-";

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
     * <p>The Converter writes to a temporary XML file in the destination
     * directory. The temporary file is moved to the requested destination only
     * when the process and basic XML validation complete successfully. Failed
     * or interrupted conversions do not leave an output XML behind.</p>
     *
     * <p>When replacement is authorized, the previous destination remains
     * unchanged until the newly generated XML has passed validation.</p>
     *
     * @param installation active validated Converter installation
     * @param request conversion request
     * @param outputListener listener notified for each Converter output line
     * @return complete conversion result
     * @throws InvalidConversionRequestException if the request is invalid
     * @throws IOException if the Converter process or output file operation
     *                     fails
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

        validateRequest(request);

        Path stagedOutputFile =
                createStagedOutputFile(request.outputFile());

        SdmxConversionRequest executionRequest =
                createExecutionRequest(
                        request,
                        stagedOutputFile
                );

        try {
            ConverterExecutionResult executionResult =
                    converterExecutor.execute(
                            installation,
                            executionRequest,
                            outputListener
                    );

            SdmxXmlValidationResult stagedValidationResult =
                    xmlOutputValidator.validate(
                            stagedOutputFile
                    );

            SdmxXmlValidationResult finalValidationResult =
                    relocateValidationResult(
                            stagedValidationResult,
                            request.outputFile()
                    );

            SdmxConversionResult conversionResult =
                    new SdmxConversionResult(
                            request,
                            executionResult,
                            finalValidationResult
                    );

            if (!conversionResult.isSuccessful()) {
                return conversionResult;
            }

            promoteStagedOutput(
                    stagedOutputFile,
                    request.outputFile(),
                    request.existingOutputPolicy()
            );

            return conversionResult;
        } finally {
            Files.deleteIfExists(stagedOutputFile);
        }
    }

    private void validateRequest(
            SdmxConversionRequest request
    ) throws InvalidConversionRequestException {
        ConversionRequestValidationResult validationResult =
                requestValidator.validate(request);

        if (!validationResult.isValid()) {
            throw new InvalidConversionRequestException(
                    validationResult
            );
        }
    }

    private Path createStagedOutputFile(
            Path outputFile
    ) {
        Path outputDirectory = outputFile.getParent();

        if (outputDirectory == null) {
            throw new IllegalArgumentException(
                    "The output file must have a parent directory"
            );
        }

        String stagedFileName =
                STAGED_FILE_PREFIX
                        + UUID.randomUUID()
                        + "-"
                        + outputFile.getFileName();

        return outputDirectory.resolve(
                stagedFileName
        );
    }

    private SdmxConversionRequest createExecutionRequest(
            SdmxConversionRequest request,
            Path stagedOutputFile
    ) {
        return new SdmxConversionRequest(
                request.inputFile(),
                stagedOutputFile,
                request.dsdFile(),
                request.headerFile(),
                request.dsdMetadata(),
                request.parameters(),
                ExistingOutputPolicy.REQUIRE_NEW
        );
    }

    private void promoteStagedOutput(
            Path stagedOutputFile,
            Path outputFile,
            ExistingOutputPolicy existingOutputPolicy
    ) throws IOException {
        try {
            moveStagedOutput(
                    stagedOutputFile,
                    outputFile,
                    existingOutputPolicy,
                    true
            );
        } catch (AtomicMoveNotSupportedException exception) {
            moveStagedOutput(
                    stagedOutputFile,
                    outputFile,
                    existingOutputPolicy,
                    false
            );
        }
    }

    private void moveStagedOutput(
            Path stagedOutputFile,
            Path outputFile,
            ExistingOutputPolicy existingOutputPolicy,
            boolean atomic
    ) throws IOException {
        if (existingOutputPolicy
                == ExistingOutputPolicy.REPLACE_EXISTING) {
            if (atomic) {
                Files.move(
                        stagedOutputFile,
                        outputFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } else {
                Files.move(
                        stagedOutputFile,
                        outputFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return;
        }

        if (atomic) {
            Files.move(
                    stagedOutputFile,
                    outputFile,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } else {
            Files.move(
                    stagedOutputFile,
                    outputFile
            );
        }
    }

    private SdmxXmlValidationResult relocateValidationResult(
            SdmxXmlValidationResult validationResult,
            Path finalOutputFile
    ) {
        return new SdmxXmlValidationResult(
                finalOutputFile,
                validationResult.fileSize(),
                validationResult.seriesCount(),
                validationResult.observationCount(),
                validationResult.errors()
        );
    }

}