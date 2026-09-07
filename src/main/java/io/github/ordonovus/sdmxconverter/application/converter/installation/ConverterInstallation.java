package io.github.ordonovus.sdmxconverter.application.converter.installation;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents an SDMX Converter installation and the private Java executable
 * assigned to it.
 *
 * @param profile compatibility profile supported by the application
 * @param installationDirectory directory containing the converter files
 * @param javaExecutable Java executable used exclusively by the converter
 */
public record ConverterInstallation(
        ConverterCompatibilityProfile profile,
        Path installationDirectory,
        Path javaExecutable
) {

    /**
     * Creates a normalized converter installation definition.
     */
    public ConverterInstallation {
        Objects.requireNonNull(profile, "profile");

        installationDirectory = normalize(
                installationDirectory,
                "installationDirectory"
        );
        javaExecutable = normalize(
                javaExecutable,
                "javaExecutable"
        );
    }

    /**
     * Returns the configured SDMX Converter version.
     *
     * @return converter version
     */
    public String converterVersion() {
        return profile.converterVersion();
    }

    /**
     * Returns the Java major version required by the converter.
     *
     * @return required Java major version
     */
    public int requiredJavaMajorVersion() {
        return profile.javaMajorVersion();
    }

    /**
     * Returns the resolved converter launcher JAR.
     *
     * @return absolute launcher JAR path
     */
    public Path launcherJar() {
        return profile.resolveLauncherJar(
                installationDirectory
        );
    }

    /**
     * Returns the directories required by the converter installation.
     *
     * @return absolute required directory paths
     */
    public List<Path> requiredDirectories() {
        return profile.resolveRequiredDirectories(
                installationDirectory
        );
    }

    /**
     * Returns the working directory used to start the converter.
     *
     * @return converter installation directory
     */
    public Path workingDirectory() {
        return installationDirectory;
    }

    private static Path normalize(
            Path path,
            String fieldName
    ) {
        return Objects.requireNonNull(
                path,
                fieldName
        ).toAbsolutePath().normalize();
    }
}