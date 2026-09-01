# SDMX Desktop Converter

Desktop application built with JavaFX to convert Excel files into SDMX-XML
using the official Eurostat SDMX Converter CLI.

## Current status

Initial project configuration.

## Technology

- Java 21
- JavaFX 21
- Maven
- SDMX Converter CLI 11.8.1
- Windows packaging with `jpackage`

## Development requirements

- JDK 21
- Maven 3.9 or later
- IntelliJ IDEA 2024.3.3 or another compatible IDE

## Running the application

```cmd
mvn clean compile
mvn javafx:run
```

## Distribution objective

The final Windows application will include its required Java runtimes. End
users will not need to install Java or configure environment variables.

## Third-party components

The application integrates with the Eurostat SDMX Converter. Its distribution,
license and notices will be documented during the packaging process.