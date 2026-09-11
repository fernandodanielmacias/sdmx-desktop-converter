package io.github.ordonovus.sdmxconverter.application.conversion;

import io.github.ordonovus.sdmxconverter.domain.model.DsdMetadata;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents all information required to perform one Excel to SDMX-XML
 * conversion.
 *
 * @param inputFile Excel input file
 * @param outputFile final destination XML file
 * @param dsdFile SDMX structure file
 * @param headerFile SDMX header properties file
 * @param dsdMetadata metadata identifying the selected DSD
 * @param parameters general Converter execution parameters
 * @param existingOutputPolicy policy applied when the destination XML exists
 */
public record SdmxConversionRequest(
        Path inputFile,
        Path outputFile,
        Path dsdFile,
        Path headerFile,
        DsdMetadata dsdMetadata,
        SdmxConversionParameters parameters,
        ExistingOutputPolicy existingOutputPolicy
) {

    /**
     * Validates and normalizes every value in the request.
     */
    public SdmxConversionRequest {
        inputFile = normalizePath(
                inputFile,
                "inputFile"
        );

        outputFile = normalizePath(
                outputFile,
                "outputFile"
        );

        dsdFile = normalizePath(
                dsdFile,
                "dsdFile"
        );

        headerFile = normalizePath(
                headerFile,
                "headerFile"
        );

        Objects.requireNonNull(
                dsdMetadata,
                "dsdMetadata"
        );

        Objects.requireNonNull(
                parameters,
                "parameters"
        );

        Objects.requireNonNull(
                existingOutputPolicy,
                "existingOutputPolicy"
        );
    }

    /**
     * Creates a request that requires a new destination XML file.
     *
     * <p>This constructor preserves compatibility for callers that do not
     * explicitly select an existing-output policy.</p>
     *
     * @param inputFile Excel input file
     * @param outputFile final destination XML file
     * @param dsdFile SDMX structure file
     * @param headerFile SDMX header properties file
     * @param dsdMetadata metadata identifying the selected DSD
     * @param parameters general Converter execution parameters
     */
    public SdmxConversionRequest(
            Path inputFile,
            Path outputFile,
            Path dsdFile,
            Path headerFile,
            DsdMetadata dsdMetadata,
            SdmxConversionParameters parameters
    ) {
        this(
                inputFile,
                outputFile,
                dsdFile,
                headerFile,
                dsdMetadata,
                parameters,
                ExistingOutputPolicy.REQUIRE_NEW
        );
    }

    /**
     * Creates a copy of this request using a different existing-output policy.
     *
     * @param policy existing-output policy assigned to the copied request
     * @return copied conversion request
     */
    public SdmxConversionRequest withExistingOutputPolicy(
            ExistingOutputPolicy policy
    ) {
        return new SdmxConversionRequest(
                inputFile,
                outputFile,
                dsdFile,
                headerFile,
                dsdMetadata,
                parameters,
                Objects.requireNonNull(policy, "policy")
        );
    }

    private static Path normalizePath(
            Path path,
            String fieldName
    ) {
        return Objects.requireNonNull(
                path,
                fieldName
        ).toAbsolutePath().normalize();
    }

}