package io.github.ordonovus.sdmxconverter.application.converter.installation;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Describes the files and Java runtime required by a supported SDMX
 * Converter version.
 *
 * @param converterVersion supported SDMX Converter version
 * @param javaMajorVersion Java major version used to run the converter
 * @param mainClass converter command-line main class
 * @param launcherJar relative path of the converter launcher JAR
 * @param requiredDirectories directories required by the converter
 */
public record ConverterCompatibilityProfile(
        String converterVersion,
        int javaMajorVersion,
        String mainClass,
        Path launcherJar,
        List<Path> requiredDirectories
) {

    /**
     * Compatibility profile for Eurostat SDMX Converter 11.8.1.
     */
    public static final ConverterCompatibilityProfile EUROSTAT_11_8_1 =
            new ConverterCompatibilityProfile(
                    "11.8.1",
                    11,
                    "com.intrasoft.sdmx.converter.ConvertAndValidateClient",
                    Path.of("converter-cli.jar"),
                    List.of(
                            Path.of("lib"),
                            Path.of("config")
                    )
            );

    /**
     * Creates and validates a converter compatibility profile.
     */
    public ConverterCompatibilityProfile {
        converterVersion = requireText(
                converterVersion,
                "converterVersion"
        );
        mainClass = requireText(
                mainClass,
                "mainClass"
        );

        if (javaMajorVersion <= 0) {
            throw new IllegalArgumentException(
                    "javaMajorVersion must be greater than zero"
            );
        }

        launcherJar = requireRelativePath(
                launcherJar,
                "launcherJar"
        );

        Objects.requireNonNull(
                requiredDirectories,
                "requiredDirectories"
        );

        requiredDirectories = requiredDirectories.stream()
                .map(path -> requireRelativePath(
                        path,
                        "requiredDirectory"
                ))
                .toList();
    }

    /**
     * Resolves the launcher JAR against an installation directory.
     *
     * @param installationDirectory converter installation directory
     * @return absolute normalized launcher JAR path
     */
    public Path resolveLauncherJar(
            Path installationDirectory
    ) {
        return normalizeInstallationDirectory(
                installationDirectory
        ).resolve(launcherJar).normalize();
    }

    /**
     * Resolves all required directories against an installation directory.
     *
     * @param installationDirectory converter installation directory
     * @return absolute normalized required directory paths
     */
    public List<Path> resolveRequiredDirectories(
            Path installationDirectory
    ) {
        Path normalizedInstallationDirectory =
                normalizeInstallationDirectory(
                        installationDirectory
                );

        return requiredDirectories.stream()
                .map(normalizedInstallationDirectory::resolve)
                .map(Path::normalize)
                .toList();
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(value, fieldName);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private static Path requireRelativePath(
            Path path,
            String fieldName
    ) {
        Objects.requireNonNull(path, fieldName);

        Path normalizedPath = path.normalize();

        if (normalizedPath.toString().isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be empty"
            );
        }

        if (normalizedPath.isAbsolute()
                || normalizedPath.startsWith("..")) {
            throw new IllegalArgumentException(
                    fieldName + " must be a safe relative path"
            );
        }

        return normalizedPath;
    }

    private static Path normalizeInstallationDirectory(
            Path installationDirectory
    ) {
        return Objects.requireNonNull(
                installationDirectory,
                "installationDirectory"
        ).toAbsolutePath().normalize();
    }
}