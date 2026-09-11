# SDMX Desktop Converter

Desktop application built with JavaFX to convert Excel files into SDMX-XML
using the official Eurostat SDMX Converter CLI.

## Current status

The application currently supports:

- Selecting one or multiple Excel files.
- Editing the generated XML file name.
- Selecting an output directory.
- Loading an SDMX Data Structure Definition.
- Loading an SDMX header properties file.
- Executing SDMX Converter CLI 11.8.1.
- Displaying batch and individual conversion progress.
- Cancelling an active conversion batch.
- Validating generated XML files.
- Counting generated series and observations.
- Safely replacing existing XML files.
- Opening the output directory.
- Displaying and exporting activity and diagnostic logs.
- Displaying a summary of the most recent conversion batch.
- Generating a self-contained Windows application image.
- Running automated tests for the conversion workflow.

## Technology

- Java 21
- JavaFX 21
- Apache Maven 3.9
- JUnit 5
- SDMX Converter CLI 11.8.1
- Java 11 private runtime for SDMX Converter
- Windows packaging with `jpackage`

## Development requirements

- IntelliJ IDEA 2024.3.3 or later
- JDK 21
- Apache Maven 3.9 or later
- Windows 10 or later

JavaFX and the testing dependencies are managed through Maven.

The project has been tested with:

- Apache Maven 3.9.16
- Eclipse Temurin JDK 21.0.12
- JavaFX 21.0.12
- Windows 11

## Java runtimes

The desktop application is developed, compiled and packaged with JDK 21.

SDMX Converter 11.8.1 is executed as an independent process using its own
private Java 11 runtime. This runtime does not modify `JAVA_HOME` and does
not need to be installed or configured by the end user.

The packaged application therefore contains two separate runtimes:

- A reduced Java 21 runtime for the JavaFX application.
- A private Java 11 runtime used exclusively by SDMX Converter.

## Local packaging dependencies

Before generating the Windows application image, the following local
directories must exist:

```text
local
├── converter
│   └── 11.8.1
│       └── app
│           └── ConverterCLIApp
└── runtime
    └── converter-java-11
```

The Converter directory must contain:

```text
local/converter/11.8.1/app/ConverterCLIApp/converter-cli.jar
```

The private Java runtime must contain:

```text
local/runtime/converter-java-11/bin/java.exe
```

These local third-party binaries are copied into the generated application
image during packaging.

## Running the application during development

From the project root, run:

```cmd
mvn javafx:run
```

The JavaFX Maven configuration uses the following application class:

```text
io.github.ordonovus.sdmxconverter.SdmxDesktopApplication
```

## Running automated tests

Run all automated tests with:

```cmd
mvn clean test
```

The test suite covers:

- Conversion request validation.
- Existing output policies.
- Safe XML replacement.
- Temporary output cleanup.
- Generated XML integrity validation.
- Secure DSD metadata parsing.
- SDMX Converter command generation.

## Generating the Windows application image

Generate a complete self-contained Windows application image with:

```cmd
mvn clean package jpackage:jpackage
```

This command:

1. Compiles the project.
2. Executes the automated tests.
3. Creates the modular application JAR.
4. Collects the JavaFX runtime dependencies.
5. Creates a reduced Java 21 runtime.
6. Copies SDMX Converter 11.8.1.
7. Copies the private Java 11 runtime.
8. Generates the native Windows executable.

The generated application is located at:

```text
target/distribution/Convertidor SDMX
```

The executable is:

```text
target/distribution/Convertidor SDMX/Convertidor SDMX.exe
```

The `target` directory contains generated build artifacts and must not be
committed to the repository.

## Running the packaged application

From Windows Command Prompt:

```cmd
"target\distribution\Convertidor SDMX\Convertidor SDMX.exe"
```

From PowerShell:

```powershell
& ".\target\distribution\Convertidor SDMX\Convertidor SDMX.exe"
```

The entire `Convertidor SDMX` directory must remain together. The executable
must not be distributed by itself.

The generated directory contains approximately:

```text
Convertidor SDMX
├── Convertidor SDMX.exe
├── app
├── runtime
├── converter
│   └── 11.8.1
│       └── ConverterCLIApp
└── converter-runtime
    └── java-11
        └── bin
            └── java.exe
```

## Packaged path resolution

The application resolves its packaged resources relative to the location of
`Convertidor SDMX.exe`.

This means that the paths are not tied to the computer used to build the
application. If the application is installed or copied to another computer,
the Converter and its private Java runtime are located relative to the new
installation directory.

The paths can also be overridden with these Java system properties:

```text
sdmx.converter.directory
sdmx.converter.java
```

## Optional modular runtime image

A standalone modular runtime image can be generated for development and
diagnostic purposes with:

```cmd
mvn clean javafx:jlink
```

This image is not the final Windows distribution. The normal distribution
workflow uses `jpackage`.

## Distribution objective

The final Windows distribution will provide a native installer and include
all required application files and Java runtimes.

End users will not need to:

- Install Java.
- Configure `JAVA_HOME`.
- Configure `PATH`.
- Install Maven.
- Execute batch files.
- Install SDMX Converter separately.

## Third-party components

The application integrates with Eurostat SDMX Converter CLI 11.8.1 and
distributes a private Java 11 runtime for executing it.

Before publishing or delivering the final installer, the applicable licenses,
copyright notices and redistribution terms for all third-party components
must be reviewed and included in the distribution.