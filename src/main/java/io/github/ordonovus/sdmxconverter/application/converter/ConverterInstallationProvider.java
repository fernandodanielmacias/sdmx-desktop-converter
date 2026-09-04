package io.github.ordonovus.sdmxconverter.application.converter;

import java.nio.file.Path;

/**
 * Provides SDMX Converter installations for development, packaged execution
 * and user-selected converter directories.
 */
public final class ConverterInstallationProvider {

    private static final String JPACKAGE_APP_PATH_PROPERTY =
            "jpackage.app-path";

    private static final String CONVERTER_DIRECTORY_PROPERTY =
            "sdmx.converter.directory";

    private static final String CONVERTER_JAVA_PROPERTY =
            "sdmx.converter.java";

    private static final Path DEVELOPMENT_CONVERTER_DIRECTORY =
            Path.of(
                    "local",
                    "converter",
                    "11.8.1",
                    "app",
                    "ConverterCLIApp"
            );

    private static final Path DEVELOPMENT_JAVA_EXECUTABLE =
            Path.of(
                    "local",
                    "runtime",
                    "converter-java-11",
                    "bin",
                    "java.exe"
            );

    private static final Path PACKAGED_CONVERTER_DIRECTORY =
            Path.of(
                    "converter",
                    "11.8.1",
                    "ConverterCLIApp"
            );

    private static final Path PACKAGED_JAVA_EXECUTABLE =
            Path.of(
                    "converter-runtime",
                    "java-11",
                    "bin",
                    "java.exe"
            );

    private final ConverterCompatibilityProfile profile;

    /**
     * Creates a provider for Eurostat SDMX Converter 11.8.1.
     */
    public ConverterInstallationProvider() {
        this(
                ConverterCompatibilityProfile.EUROSTAT_11_8_1
        );
    }

    /**
     * Creates a provider for a specific compatibility profile.
     *
     * @param profile supported converter compatibility profile
     */
    public ConverterInstallationProvider(
            ConverterCompatibilityProfile profile
    ) {
        this.profile = profile;
    }

    /**
     * Resolves the default converter installation for the current execution
     * environment.
     *
     * @return development or packaged converter installation
     */
    public ConverterInstallation getDefaultInstallation() {
        Path applicationDirectory =
                resolveApplicationDirectory();

        Path converterDirectory =
                resolveConfiguredPath(
                        CONVERTER_DIRECTORY_PROPERTY,
                        applicationDirectory.resolve(
                                isPackagedApplication()
                                        ? PACKAGED_CONVERTER_DIRECTORY
                                        : DEVELOPMENT_CONVERTER_DIRECTORY
                        )
                );

        Path javaExecutable =
                resolveConfiguredPath(
                        CONVERTER_JAVA_PROPERTY,
                        applicationDirectory.resolve(
                                isPackagedApplication()
                                        ? PACKAGED_JAVA_EXECUTABLE
                                        : DEVELOPMENT_JAVA_EXECUTABLE
                        )
                );

        return new ConverterInstallation(
                profile,
                converterDirectory,
                javaExecutable
        );
    }

    /**
     * Creates an installation using a user-selected converter directory while
     * retaining the Java runtime managed by the application.
     *
     * @param converterDirectory selected converter installation directory
     * @return converter installation using the managed Java runtime
     */
    public ConverterInstallation fromSelectedDirectory(
            Path converterDirectory
    ) {
        ConverterInstallation defaultInstallation =
                getDefaultInstallation();

        return new ConverterInstallation(
                profile,
                converterDirectory,
                defaultInstallation.javaExecutable()
        );
    }

    private Path resolveApplicationDirectory() {
        String packagedApplicationPath =
                System.getProperty(JPACKAGE_APP_PATH_PROPERTY);

        if (packagedApplicationPath == null
                || packagedApplicationPath.isBlank()) {
            return Path.of("")
                    .toAbsolutePath()
                    .normalize();
        }

        Path launcherPath = Path.of(packagedApplicationPath)
                .toAbsolutePath()
                .normalize();

        Path parent = launcherPath.getParent();

        return parent == null
                ? launcherPath
                : parent;
    }

    private boolean isPackagedApplication() {
        String packagedApplicationPath =
                System.getProperty(JPACKAGE_APP_PATH_PROPERTY);

        return packagedApplicationPath != null
                && !packagedApplicationPath.isBlank();
    }

    private Path resolveConfiguredPath(
            String propertyName,
            Path defaultPath
    ) {
        String configuredValue =
                System.getProperty(propertyName);

        if (configuredValue == null
                || configuredValue.isBlank()) {
            return defaultPath.toAbsolutePath().normalize();
        }

        return Path.of(configuredValue)
                .toAbsolutePath()
                .normalize();
    }
}