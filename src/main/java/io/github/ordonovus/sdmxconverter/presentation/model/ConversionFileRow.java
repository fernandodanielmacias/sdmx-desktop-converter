package io.github.ordonovus.sdmxconverter.presentation.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents an input file displayed in the conversion queue.
 */
public final class ConversionFileRow {

    private static final String DEFAULT_STATUS = "Pendiente";
    private static final String EMPTY_RESULT = "—";

    private final Path path;
    private final ReadOnlyStringWrapper fileName;
    private final ReadOnlyStringWrapper filePath;
    private final StringProperty outputFileName;
    private final StringProperty status;
    private final ReadOnlyStringWrapper seriesCount;
    private final ReadOnlyStringWrapper observationCount;

    /**
     * Creates a row for an Excel file selected by the user.
     *
     * @param path absolute or relative path of the selected file
     */
    public ConversionFileRow(Path path) {
        this.path = Objects.requireNonNull(path, "path")
                .toAbsolutePath()
                .normalize();

        String inputFileName = this.path.getFileName().toString();

        this.fileName = new ReadOnlyStringWrapper(inputFileName);
        this.filePath = new ReadOnlyStringWrapper(this.path.toString());
        this.outputFileName = new SimpleStringProperty(
                createDefaultOutputFileName(inputFileName)
        );
        this.status = new SimpleStringProperty(DEFAULT_STATUS);
        this.seriesCount = new ReadOnlyStringWrapper(EMPTY_RESULT);
        this.observationCount = new ReadOnlyStringWrapper(EMPTY_RESULT);
    }

    /**
     * Returns the normalized path of the input file.
     *
     * @return selected file path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Provides the file name property used by the JavaFX table.
     *
     * @return read-only file name property
     */
    public ReadOnlyStringProperty fileNameProperty() {
        return fileName.getReadOnlyProperty();
    }

    /**
     * Provides the complete path property used by the JavaFX table.
     *
     * @return read-only file path property
     */
    public ReadOnlyStringProperty filePathProperty() {
        return filePath.getReadOnlyProperty();
    }

    /**
     * Provides the editable XML output file name property.
     *
     * @return observable output file name property
     */
    public StringProperty outputFileNameProperty() {
        return outputFileName;
    }

    /**
     * Returns the configured XML output file name.
     *
     * @return output file name
     */
    public String getOutputFileName() {
        return outputFileName.get();
    }

    /**
     * Updates the XML output file name.
     *
     * @param value new XML output file name
     */
    public void setOutputFileName(String value) {
        outputFileName.set(
                Objects.requireNonNull(value, "value")
        );
    }

    /**
     * Provides the current conversion status property.
     *
     * @return observable status property
     */
    public StringProperty statusProperty() {
        return status;
    }

    /**
     * Returns the current conversion status.
     *
     * @return current status text
     */
    public String getStatus() {
        return status.get();
    }

    /**
     * Updates the status displayed for this file.
     *
     * @param value new status text
     */
    public void setStatus(String value) {
        status.set(
                Objects.requireNonNull(value, "value")
        );
    }

    /**
     * Provides the generated series count property.
     *
     * @return read-only series count property
     */
    public ReadOnlyStringProperty seriesCountProperty() {
        return seriesCount.getReadOnlyProperty();
    }

    /**
     * Provides the generated observation count property.
     *
     * @return read-only observation count property
     */
    public ReadOnlyStringProperty observationCountProperty() {
        return observationCount.getReadOnlyProperty();
    }

    /**
     * Stores the counts obtained from a successful XML conversion.
     *
     * @param series generated series count
     * @param observations generated observation count
     */
    public void setConversionCounts(
            long series,
            long observations
    ) {
        if (series < 0 || observations < 0) {
            throw new IllegalArgumentException(
                    "Conversion counts must not be negative"
            );
        }

        seriesCount.set(Long.toString(series));
        observationCount.set(Long.toString(observations));
    }

    /**
     * Clears previously generated conversion counts.
     */
    public void clearConversionCounts() {
        seriesCount.set(EMPTY_RESULT);
        observationCount.set(EMPTY_RESULT);
    }

    private static String createDefaultOutputFileName(
            String inputFileName
    ) {
        int extensionIndex = inputFileName.lastIndexOf('.');

        String baseName = extensionIndex > 0
                ? inputFileName.substring(0, extensionIndex)
                : inputFileName;

        return baseName + ".xml";
    }

}