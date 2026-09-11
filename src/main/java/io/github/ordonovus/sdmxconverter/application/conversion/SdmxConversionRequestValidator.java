package io.github.ordonovus.sdmxconverter.application.conversion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Performs the validations required before starting an SDMX conversion.
 */
public final class SdmxConversionRequestValidator {

    private static final Set<String> SUPPORTED_EXCEL_EXTENSIONS =
            Set.of(".xls", ".xlsx");

    private static final String REQUIRED_INPUT_FORMAT = "EXCEL";
    private static final String REQUIRED_OUTPUT_FORMAT = "COMPACT_SDMX";

    /**
     * Validates one conversion request.
     *
     * @param request conversion request to validate
     * @return validation result containing every detected error
     */
    public ConversionRequestValidationResult validate(
            SdmxConversionRequest request
    ) {
        List<String> errors = new ArrayList<>();

        validateInputFile(request.inputFile(), errors);
        validateOutputFile(request, errors);
        validateDsdFile(request.dsdFile(), errors);
        validateHeaderFile(request.headerFile(), errors);
        validateParameters(request.parameters(), errors);

        return new ConversionRequestValidationResult(errors);
    }

    private void validateInputFile(
            Path inputFile,
            List<String> errors
    ) {
        validateReadableFile(
                inputFile,
                "El archivo Excel de entrada",
                errors
        );

        if (hasUnsupportedExtension(inputFile, SUPPORTED_EXCEL_EXTENSIONS)) {
            errors.add(
                    "El archivo de entrada debe tener extensión "
                            + ".xls o .xlsx."
            );
        }
    }

    private void validateOutputFile(
            SdmxConversionRequest request,
            List<String> errors
    ) {
        Path outputFile = request.outputFile();

        if (hasUnsupportedExtension(outputFile, Set.of(".xml"))) {
            errors.add(
                    "El archivo de salida debe tener extensión .xml."
            );
        }

        if (request.inputFile().equals(outputFile)) {
            errors.add(
                    "El archivo de salida no puede ser igual "
                            + "al archivo de entrada."
            );
        }

        Path outputDirectory = outputFile.getParent();

        if (outputDirectory == null) {
            errors.add(
                    "No fue posible determinar la carpeta de salida."
            );
            return;
        }

        if (!Files.exists(outputDirectory)) {
            errors.add(
                    "La carpeta de salida no existe: "
                            + outputDirectory
            );
            return;
        }

        if (!Files.isDirectory(outputDirectory)) {
            errors.add(
                    "La ruta de salida no corresponde a una carpeta: "
                            + outputDirectory
            );
            return;
        }

        if (!Files.isWritable(outputDirectory)) {
            errors.add(
                    "No se tienen permisos para escribir en la carpeta "
                            + "de salida: "
                            + outputDirectory
            );
        }

        validateExistingOutput(request, errors);
    }

    private void validateExistingOutput(
            SdmxConversionRequest request,
            List<String> errors
    ) {
        Path outputFile = request.outputFile();

        if (!Files.exists(outputFile)) {
            return;
        }

        if (!Files.isRegularFile(outputFile)) {
            errors.add(
                    "La ruta del archivo XML de salida ya existe, "
                            + "pero no corresponde a un archivo: "
                            + outputFile
            );
            return;
        }

        if (request.existingOutputPolicy()
                == ExistingOutputPolicy.REQUIRE_NEW) {
            errors.add(
                    "El archivo XML de salida ya existe: "
                            + outputFile.getFileName()
                            + ". Cambie el nombre, omita el archivo "
                            + "o autorice su reemplazo."
            );
            return;
        }

        if (!Files.isWritable(outputFile)) {
            errors.add(
                    "El archivo XML existente no se puede reemplazar: "
                            + outputFile
            );
        }
    }

    private void validateDsdFile(
            Path dsdFile,
            List<String> errors
    ) {
        validateReadableFile(
                dsdFile,
                "El archivo DSD",
                errors
        );

        if (hasUnsupportedExtension(dsdFile, Set.of(".xml"))) {
            errors.add(
                    "El archivo DSD debe tener extensión .xml."
            );
        }
    }

    private void validateHeaderFile(
            Path headerFile,
            List<String> errors
    ) {
        validateReadableFile(
                headerFile,
                "El archivo de encabezado",
                errors
        );

        if (hasUnsupportedExtension(headerFile, Set.of(".prop"))) {
            errors.add(
                    "El archivo de encabezado debe tener "
                            + "extensión .prop."
            );
        }
    }

    private void validateParameters(
            SdmxConversionParameters parameters,
            List<String> errors
    ) {
        if (!REQUIRED_INPUT_FORMAT.equals(
                parameters.inputFormat()
        )) {
            errors.add(
                    "El formato de entrada configurado debe ser EXCEL."
            );
        }

        if (!REQUIRED_OUTPUT_FORMAT.equals(
                parameters.outputFormat()
        )) {
            errors.add(
                    "El formato de salida configurado debe ser "
                            + "COMPACT_SDMX."
            );
        }

        if (!parameters.validation()) {
            errors.add(
                    "La validación SDMX debe permanecer habilitada."
            );
        }

        if (parameters.useRegistry()) {
            errors.add(
                    "El uso del Registry debe permanecer deshabilitado "
                            + "para conversiones con un DSD local."
            );
        }

        if (!parameters.errorIfEmpty()) {
            errors.add(
                    "La validación de archivos sin observaciones debe "
                            + "permanecer habilitada."
            );
        }

        if (!parameters.groupedOutput()) {
            errors.add(
                    "La salida agrupada debe permanecer habilitada."
            );
        }
    }

    private void validateReadableFile(
            Path file,
            String description,
            List<String> errors
    ) {
        if (!Files.exists(file)) {
            errors.add(
                    description + " no existe: " + file
            );
            return;
        }

        if (!Files.isRegularFile(file)) {
            errors.add(
                    description
                            + " no corresponde a un archivo: "
                            + file
            );
            return;
        }

        if (!Files.isReadable(file)) {
            errors.add(
                    description + " no se puede leer: " + file
            );
        }
    }

    private boolean hasUnsupportedExtension(
            Path file,
            Set<String> supportedExtensions
    ) {
        String fileName = file.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        return supportedExtensions.stream()
                .noneMatch(fileName::endsWith);
    }

}