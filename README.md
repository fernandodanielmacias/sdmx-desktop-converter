# SDMX Desktop Converter

JavaFX desktop application for converting Excel files into SDMX-XML using
Eurostat SDMX Converter CLI 11.8.1.

## Technology

- Java 21
- JavaFX 21
- Maven 3.9 or later
- Eurostat SDMX Converter CLI 11.8.1
- Private Java 11 runtime for the converter
- Inno Setup 7 for Windows installer generation

## Development requirements

- JDK 21
- Apache Maven 3.9 or later
- Windows 10 or later

JavaFX dependencies are managed through Maven.

The project has been tested with Apache Maven 3.9.16.

## Java runtimes

The desktop application is compiled and executed with Java 21.

Eurostat SDMX Converter 11.8.1 runs with its own private Java 11 runtime.
This runtime does not modify `JAVA_HOME` and does not need to be installed
or configured by the end user.

## Local converter files

The development environment expects the converter installation at:

```text
local/converter/11.8.1/app/ConverterCLIApp
```

The private Java 11 runtime is expected at:

```text
local/runtime/converter-java-11
```

These files are copied into the packaged application when the Windows
distribution is generated.

## Running the application

Compile and run the application from the project directory:

```cmd
mvn clean compile
mvn javafx:run
```

## Running tests

Execute the automated test suite with:

```cmd
mvn clean test
```

## Creating the Windows installer

### Requirements

Install Inno Setup 7 and add its installation directory permanently to the
user `PATH` environment variable:

```text
C:\Program Files\Inno Setup 7
```

Verify the compiler installation:

```cmd
where ISCC.exe
ISCC.exe /?
```

### Build command

Generate and test the application, create the Java application image and
build the Windows installer with:

```cmd
mvn clean verify -Pwindows-installer
```

The generated installer is located at:

```text
target\installer\Convertidor-SDMX-1.0.0.exe
```

The intermediate application image is located at:

```text
target\distribution\Convertidor SDMX
```

The installer includes:

- The Java 21 application runtime.
- Eurostat SDMX Converter CLI 11.8.1.
- The private Java 11 runtime required by the converter.
- Start menu and optional desktop shortcuts.
- Spanish installation and uninstallation interfaces.

End users do not need to install Java, Maven, JavaFX, Inno Setup or configure
environment variables.

## SDMX structures

The application reads the identity of the selected SDMX Data Structure
Definition, including its agency, identifier and version.

The current Excel template has been validated with:

```text
ESTAT:NA_MAIN(1.17.0)
```

Other DSD versions may require additional SDMX structures, such as concept
schemes and code lists, as well as changes to the Excel input structure.

## Third-party components

The application integrates Eurostat SDMX Converter CLI. Its distribution and
use must comply with the licenses and notices supplied with the converter.

Inno Setup is used only during the installer build process and is not included
in the installed application.