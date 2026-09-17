# SDMX Desktop Converter

JavaFX desktop application for converting Excel files into SDMX-XML using
Eurostat SDMX Converter CLI 11.8.1.

## Technology

- Java 21
- JavaFX 21
- Maven 3.9 or later
- Eurostat SDMX Converter CLI 11.8.1
- Eclipse Temurin Java 11 runtime for the converter
- Inno Setup 7 for Windows installer generation

## Development requirements

- JDK 21
- Apache Maven 3.9 or later
- Windows 10 or later

JavaFX dependencies are managed through Maven.

The project has been tested with Apache Maven 3.9.16.

## Java runtimes

The desktop application is compiled and executed with Java 21.

Eurostat SDMX Converter 11.8.1 runs with its own private Eclipse Temurin
Java 11 runtime. This runtime does not modify `JAVA_HOME` and does not need
to be installed or configured by the end user.

The packaged Java 11 runtime version is:

```text
Eclipse Temurin 11.0.32.1+1
```

## Local converter files

The development environment expects the converter installation at:

```text
local/converter/11.8.1/app/ConverterCLIApp
```

The corresponding Converter CLI source archive is expected at:

```text
local/converter/11.8.1/src/converter-cli-11.8.1-sources.jar
```

The original SDMX Converter license notice is expected at:

```text
local/converter/11.8.1/license.txt
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

Restart IntelliJ IDEA after modifying the environment variable.

Verify that the Inno Setup compiler is available:

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
target\installer\Convertidor-SDMX-1.0.2.exe
```

The intermediate application image is located at:

```text
target\distribution\Convertidor SDMX
```

The installer includes:

- The Java 21 application runtime.
- Eurostat SDMX Converter CLI 11.8.1.
- The Converter CLI 11.8.1 source archive.
- The private Eclipse Temurin Java 11 runtime required by the converter.
- Applicable license and third-party notice files.
- Start menu and optional desktop shortcuts.
- Spanish installation and uninstallation interfaces.

End users do not need to install Java, Maven, JavaFX, Inno Setup or configure
environment variables.

## SDMX structures

The application reads the identity of the selected SDMX Data Structure
Definition, including its agency, identifier and version.

The current Excel template has been successfully validated with:

```text
ESTAT:NA_MAIN(1.17.0)
ESTAT:NA_MAIN(1.18.0)
```

A DSD downloaded without its referenced structures may fail during conversion
because the converter cannot resolve its concept schemes or code lists.

When downloading a DSD from the SDMX Global Registry for local use, request
its child references:

```text
https://registry.sdmx.org/ws/public/sdmxapi/rest/datastructure/{agency}/{id}/{version}?references=children
```

For example:

```text
https://registry.sdmx.org/ws/public/sdmxapi/rest/datastructure/ESTAT/NA_MAIN/1.18.0?references=children
```

This response includes the DSD and the concept schemes and code lists required
by the converter. The selected XML remains local, so conversion does not
depend on registry or Internet availability.

## License

SDMX Desktop Converter source code is copyright 2026 Ordo Novus and is
licensed under the Apache License 2.0.

See the following files for details:

```text
LICENSE
NOTICE
```

The Apache License 2.0 applies only to the original SDMX Desktop Converter
source code. Third-party components retain their respective licenses and are
not relicensed under Apache License 2.0.

## Third-party components

### Eurostat SDMX Converter

SDMX Converter is copyright 2009 by the European Community, represented by
Eurostat, and is distributed under the European Union Public Licence 1.1.

The packaged application preserves:

```text
converter/11.8.1/ConverterCLIApp/license.txt
converter/11.8.1/src/converter-cli-11.8.1-sources.jar
licenses/EUPL-1.1.txt
```

The original license notice identifies the third-party libraries used by the
converter and their corresponding licenses.

### Eclipse Temurin

The packaged Java 11 runtime is Eclipse Temurin 11.0.32.1+1, provided by the
Eclipse Adoptium project.

The runtime is distributed with its original `NOTICE` file and the license,
additional license information and assembly exception files for its included
modules.

### OpenJFX

The application uses OpenJFX 21.0.12. Its applicable notices and legal
information are preserved in the packaged application runtime and
dependencies.

### Inno Setup

Inno Setup is used only during the Windows installer build process. It is not
included in the installed application.