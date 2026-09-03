package io.github.ordonovus.sdmxconverter.application.service;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Normalizes and validates XML output file names.
 */
public final class OutputFileNameService {

    private static final String XML_EXTENSION = ".xml";
    private static final int MAXIMUM_FILE_NAME_LENGTH = 255;

    private static final Pattern INVALID_WINDOWS_FILE_CHARACTERS =
            Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");

    private static final Pattern RESERVED_WINDOWS_FILE_NAME =
            Pattern.compile(
                    "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * Normalizes and validates an XML output file name.
     *
     * <p>The XML extension is appended when it is not supplied by the user.</p>
     *
     * @param value output file name entered by the user
     * @return validated file name ending in {@code .xml}
     * @throws IllegalArgumentException when the name is invalid on Windows
     */
    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del archivo de salida no puede estar vacío."
            );
        }

        String normalizedValue = value.trim();

        if (INVALID_WINDOWS_FILE_CHARACTERS
                .matcher(normalizedValue)
                .find()) {
            throw new IllegalArgumentException(
                    "El nombre contiene caracteres no permitidos por Windows."
            );
        }

        if (normalizedValue.endsWith(".")) {
            throw new IllegalArgumentException(
                    "El nombre del archivo no puede terminar con un punto."
            );
        }

        if (!normalizedValue.toLowerCase(Locale.ROOT)
                .endsWith(XML_EXTENSION)) {
            normalizedValue += XML_EXTENSION;
        }

        String baseName = normalizedValue.substring(
                0,
                normalizedValue.length() - XML_EXTENSION.length()
        );

        if (baseName.isBlank()) {
            throw new IllegalArgumentException(
                    "El archivo debe tener un nombre antes de la extensión."
            );
        }

        String deviceName = baseName
                .split("\\.", 2)[0];

        if (RESERVED_WINDOWS_FILE_NAME
                .matcher(deviceName)
                .matches()) {
            throw new IllegalArgumentException(
                    "El nombre está reservado por Windows."
            );
        }

        if (normalizedValue.length() > MAXIMUM_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre del archivo es demasiado largo."
            );
        }

        return normalizedValue;
    }

    /**
     * Validates that an output file name is not already in use.
     *
     * @param outputFileName normalized output file name
     * @param existingFileNames names already assigned to other files
     * @throws IllegalArgumentException when the name is already in use
     */
    public void validateUnique(
            String outputFileName,
            Collection<String> existingFileNames
    ) {
        Objects.requireNonNull(
                existingFileNames,
                "existingFileNames"
        );

        if (containsIgnoreCase(
                existingFileNames,
                outputFileName
        )) {
            throw new IllegalArgumentException(
                    "Ya existe otro archivo de salida con ese nombre."
            );
        }
    }

    /**
     * Creates a unique XML output file name by adding a numeric suffix when
     * necessary.
     *
     * @param preferredFileName preferred output file name
     * @param existingFileNames names already assigned to other files
     * @return an available normalized output file name
     */
    public String createUnique(
            String preferredFileName,
            Collection<String> existingFileNames
    ) {
        Objects.requireNonNull(
                existingFileNames,
                "existingFileNames"
        );

        String normalizedFileName =
                normalize(preferredFileName);

        if (!containsIgnoreCase(
                existingFileNames,
                normalizedFileName
        )) {
            return normalizedFileName;
        }

        String baseName = normalizedFileName.substring(
                0,
                normalizedFileName.length() - XML_EXTENSION.length()
        );

        int suffix = 2;
        String candidate;

        do {
            candidate = "%s (%d)%s".formatted(
                    baseName,
                    suffix,
                    XML_EXTENSION
            );
            suffix++;
        } while (containsIgnoreCase(
                existingFileNames,
                candidate
        ));

        return candidate;
    }

    private boolean containsIgnoreCase(
            Collection<String> fileNames,
            String candidate
    ) {
        Objects.requireNonNull(
                candidate,
                "candidate"
        );

        return fileNames.stream()
                .filter(Objects::nonNull)
                .anyMatch(fileName ->
                        fileName.equalsIgnoreCase(candidate)
                );
    }
}
