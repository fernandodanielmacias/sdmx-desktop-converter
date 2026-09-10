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

- IntelliJ IDEA 2024.3.3 or later
- JDK 21
- Apache Maven 3.9 or later
- JavaFX 21, managed through Maven
- Windows 10 or later

The project has been tested with Apache Maven 3.9.16.

## Java runtimes

The desktop application is developed and compiled with JDK 21.

SDMX Converter 11.8.1 is executed with its own private Java 11 runtime.
This runtime does not modify `JAVA_HOME` and does not need to be installed
or configured by the end user.

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