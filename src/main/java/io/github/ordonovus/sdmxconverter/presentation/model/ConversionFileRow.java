package io.github.ordonovus.sdmxconverter.presentation.model;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents an input file displayed in the conversion queue.
 */
public final class ConversionFileRow {

    private static final String DEFAULT_STATUS = "Pendiente";

    private final Path path;
    private final ReadOnlyStringWrapper fileName;
    private final ReadOnlyStringWrapper filePath;
    private final StringProperty status;

    /**
     * Creates a row for an Excel file selected by the user.
     *
     * @param path absolute or relative path of the selected file
     */
    public ConversionFileRow(Path path) {
        this.path = Objects.requireNonNull(path, "path")
                .toAbsolutePath()
                .normalize();

        this.fileName = new ReadOnlyStringWrapper(
                this.path.getFileName().toString()
        );
        this.filePath = new ReadOnlyStringWrapper(this.path.toString());
        this.status = new ReadOnlyStringWrapper(DEFAULT_STATUS);
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
     * Provides the current conversion status property.
     *
     * @return observable status property
     */
    public StringProperty statusProperty() {
        return status;
    }

    /**
     * Updates the status displayed for this file.
     *
     * @param value new status text
     */
    public void setStatus(String value) {
        status.set(Objects.requireNonNull(value, "value"));
    }

}
