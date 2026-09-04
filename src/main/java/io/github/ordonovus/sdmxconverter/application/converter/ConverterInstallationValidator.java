package io.github.ordonovus.sdmxconverter.application.converter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Validates the files and Java runtime assigned to an SDMX Converter
 * installation.
 */
public final class ConverterInstallationValidator {

    private static final int JAVA_VERSION_TIMEOUT_SECONDS = 10;

    private static final Pattern JAVA_VERSION_PATTERN =
            Pattern.compile(
                    "version\\s+\"(?:1\\.)?(\\d+)"
            );

    /**
     * Validates a converter installation against its compatibility profile.
     *
     * @param installation converter installation to validate
     * @return validation errors and warnings
     */
    public ConverterInstallationValidationResult validate(
            ConverterInstallation installation
    ) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateInstallationDirectory(
                installation,
                errors
        );
        validateLauncherJar(
                installation,
                errors,
                warnings
        );
        validateRequiredDirectories(
                installation,
                errors,
                warnings
        );
        validateJavaRuntime(
                installation,
                errors
        );

        return new ConverterInstallationValidationResult(
                errors,
                warnings
        );
    }

    private void validateInstallationDirectory(
            ConverterInstallation installation,
            List<String> errors
    ) {
        Path directory = installation.installationDirectory();

        if (!Files.isDirectory(directory)) {
            errors.add(
                    "La carpeta del convertidor no existe."
            );
            return;
        }

        if (!Files.isReadable(directory)) {
            errors.add(
                    "No se puede leer la carpeta del convertidor."
            );
        }
    }

    private void validateLauncherJar(
            ConverterInstallation installation,
            List<String> errors,
            List<String> warnings
    ) {
        Path launcherJar = installation.launcherJar();

        if (!Files.isRegularFile(launcherJar)) {
            errors.add(
                    "No se encontró converter-cli.jar."
            );
            return;
        }

        if (!Files.isReadable(launcherJar)) {
            errors.add(
                    "No se puede leer converter-cli.jar."
            );
            return;
        }

        inspectLauncherJar(
                installation,
                errors,
                warnings
        );
    }

    private void inspectLauncherJar(
            ConverterInstallation installation,
            List<String> errors,
            List<String> warnings
    ) {
        try (var jarFile =
                     new JarFile(
                             installation.launcherJar().toFile()
                     )) {

            String mainClassEntry = installation.profile()
                    .mainClass()
                    .replace('.', '/')
                    + ".class";

            if (jarFile.getJarEntry(mainClassEntry) == null) {
                errors.add(
                        "converter-cli.jar no contiene "
                                + "la clase principal esperada."
                );
            }

            Optional<String> detectedVersion =
                    detectManifestVersion(jarFile);

            if (detectedVersion.isEmpty()) {
                warnings.add(
                        "No fue posible confirmar automáticamente "
                                + "la versión interna del convertidor."
                );
                return;
            }

            if (!detectedVersion.get().equals(
                    installation.converterVersion()
            )) {
                errors.add(
                        "La versión detectada del convertidor es "
                                + detectedVersion.get()
                                + ", pero se esperaba "
                                + installation.converterVersion()
                                + "."
                );
            }
        } catch (IOException exception) {
            errors.add(
                    "converter-cli.jar no es un archivo JAR válido."
            );
        }
    }

    private Optional<String> detectManifestVersion(
            JarFile jarFile
    ) throws IOException {
        if (jarFile.getManifest() == null) {
            return Optional.empty();
        }

        Attributes attributes = jarFile.getManifest()
                .getMainAttributes();

        List<Attributes.Name> versionAttributes = List.of(
                Attributes.Name.IMPLEMENTATION_VERSION,
                Attributes.Name.SPECIFICATION_VERSION
        );

        for (Attributes.Name attribute : versionAttributes) {
            String value = attributes.getValue(attribute);

            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }

        String bundleVersion =
                attributes.getValue("Bundle-Version");

        return bundleVersion == null || bundleVersion.isBlank()
                ? Optional.empty()
                : Optional.of(bundleVersion.trim());
    }

    private void validateRequiredDirectories(
            ConverterInstallation installation,
            List<String> errors,
            List<String> warnings
    ) {
        for (Path directory :
                installation.requiredDirectories()) {

            if (!Files.isDirectory(directory)) {
                errors.add(
                        "No se encontró la carpeta requerida: "
                                + directory.getFileName() + "."
                );
                continue;
            }

            if (!Files.isReadable(directory)) {
                errors.add(
                        "No se puede leer la carpeta requerida: "
                                + directory.getFileName() + "."
                );
            }
        }

        validateLibraryDirectory(
                installation.installationDirectory()
                        .resolve("lib"),
                errors
        );

        validateConfigurationDirectory(
                installation.installationDirectory()
                        .resolve("config"),
                warnings
        );
    }

    private void validateLibraryDirectory(
            Path libraryDirectory,
            List<String> errors
    ) {
        if (!Files.isDirectory(libraryDirectory)) {
            return;
        }

        try (Stream<Path> files =
                     Files.list(libraryDirectory)) {

            boolean containsJar = files.anyMatch(
                    this::isJarFile
            );

            if (!containsJar) {
                errors.add(
                        "La carpeta lib no contiene "
                                + "las dependencias JAR requeridas."
                );
            }
        } catch (IOException exception) {
            errors.add(
                    "No fue posible revisar la carpeta lib."
            );
        }
    }

    private void validateConfigurationDirectory(
            Path configurationDirectory,
            List<String> warnings
    ) {
        if (!Files.isDirectory(configurationDirectory)) {
            return;
        }

        try (Stream<Path> files =
                     Files.list(configurationDirectory)) {

            if (files.findAny().isEmpty()) {
                warnings.add(
                        "La carpeta config está vacía."
                );
            }
        } catch (IOException exception) {
            warnings.add(
                    "No fue posible revisar el contenido "
                            + "de la carpeta config."
            );
        }
    }

    private void validateJavaRuntime(
            ConverterInstallation installation,
            List<String> errors
    ) {
        Path javaExecutable = installation.javaExecutable();

        if (!Files.isRegularFile(javaExecutable)) {
            errors.add(
                    "No se encontró el ejecutable privado de Java."
            );
            return;
        }

        if (!Files.isReadable(javaExecutable)) {
            errors.add(
                    "No se puede leer el ejecutable privado de Java."
            );
            return;
        }

        try {
            int detectedJavaVersion =
                    detectJavaMajorVersion(javaExecutable);

            if (detectedJavaVersion
                    != installation.requiredJavaMajorVersion()) {
                errors.add(
                        "El convertidor requiere Java "
                                + installation.requiredJavaMajorVersion()
                                + ", pero se detectó Java "
                                + detectedJavaVersion + "."
                );
            }
        } catch (IOException exception) {
            errors.add(
                    "No fue posible iniciar el Java privado "
                            + "del convertidor."
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            errors.add(
                    "La validación de Java fue interrumpida."
            );
        }
    }

    private int detectJavaMajorVersion(
            Path javaExecutable
    ) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                javaExecutable.toString(),
                "-version"
        ).redirectErrorStream(true).start();

        boolean finished = process.waitFor(
                JAVA_VERSION_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );

        if (!finished) {
            process.destroyForcibly();

            throw new IOException(
                    "Java version command timed out"
            );
        }

        String output;

        try (InputStream input = process.getInputStream()) {
            output = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        if (process.exitValue() != 0) {
            throw new IOException(
                    "Java version command failed"
            );
        }

        Matcher matcher =
                JAVA_VERSION_PATTERN.matcher(output);

        if (!matcher.find()) {
            throw new IOException(
                    "Java version could not be parsed"
            );
        }

        return Integer.parseInt(matcher.group(1));
    }

    private boolean isJarFile(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName()
                .toString()
                .toLowerCase()
                .endsWith(".jar");
    }
}