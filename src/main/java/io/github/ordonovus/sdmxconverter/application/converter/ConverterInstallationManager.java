package io.github.ordonovus.sdmxconverter.application.converter;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Manages the active SDMX Converter installation and coordinates its
 * validation.
 */
public final class ConverterInstallationManager {

    private final ConverterInstallationProvider installationProvider;
    private final ConverterInstallationValidator installationValidator;

    private ConverterInstallation activeInstallation;

    /**
     * Creates a manager using the default installation provider and
     * validator.
     */
    public ConverterInstallationManager() {
        this(
                new ConverterInstallationProvider(),
                new ConverterInstallationValidator()
        );
    }

    /**
     * Creates a manager with explicit dependencies.
     *
     * @param installationProvider provider used to locate installations
     * @param installationValidator validator used to verify installations
     */
    public ConverterInstallationManager(
            ConverterInstallationProvider installationProvider,
            ConverterInstallationValidator installationValidator
    ) {
        this.installationProvider = Objects.requireNonNull(
                installationProvider,
                "installationProvider"
        );
        this.installationValidator = Objects.requireNonNull(
                installationValidator,
                "installationValidator"
        );
        this.activeInstallation =
                installationProvider.getDefaultInstallation();
    }

    /**
     * Returns the currently active converter installation.
     *
     * @return active converter installation
     */
    public synchronized ConverterInstallation getActiveInstallation() {
        return activeInstallation;
    }

    /**
     * Validates the currently active converter installation.
     *
     * @return validation result
     */
    public synchronized ConverterInstallationValidationResult
    validateActiveInstallation() {
        return installationValidator.validate(
                activeInstallation
        );
    }

    /**
     * Validates a user-selected converter directory and activates it only
     * when it is compatible.
     *
     * @param converterDirectory selected converter installation directory
     * @return validation result for the selected installation
     */
    public synchronized ConverterInstallationValidationResult
    selectInstallation(Path converterDirectory) {
        Objects.requireNonNull(
                converterDirectory,
                "converterDirectory"
        );

        ConverterInstallation candidate =
                installationProvider.fromSelectedDirectory(
                        converterDirectory
                );

        ConverterInstallationValidationResult result =
                installationValidator.validate(candidate);

        if (result.isValid()) {
            activeInstallation = candidate;
        }

        return result;
    }

    /**
     * Restores and validates the converter installation included with the
     * application.
     *
     * @return validation result for the default installation
     */
    public synchronized ConverterInstallationValidationResult
    restoreDefaultInstallation() {
        activeInstallation =
                installationProvider.getDefaultInstallation();

        return installationValidator.validate(
                activeInstallation
        );
    }
}