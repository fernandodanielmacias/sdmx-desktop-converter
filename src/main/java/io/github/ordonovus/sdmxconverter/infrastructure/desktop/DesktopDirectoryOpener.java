package io.github.ordonovus.sdmxconverter.infrastructure.desktop;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Opens a directory using the file manager provided by the operating system.
 */
public final class DesktopDirectoryOpener {

    /**
     * Opens the specified directory in the operating system file manager.
     *
     * @param directory directory to open
     * @throws IOException if the directory cannot be opened
     */
    public void open(Path directory) throws IOException {
        Path normalizedDirectory =
                Objects.requireNonNull(directory, "directory")
                        .toAbsolutePath()
                        .normalize();

        if (!Files.isDirectory(normalizedDirectory)) {
            throw new IOException(
                    "The directory does not exist: "
                            + normalizedDirectory
            );
        }

        if (!Desktop.isDesktopSupported()) {
            throw new IOException(
                    "Desktop operations are not supported"
            );
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            throw new IOException(
                    "Opening directories is not supported"
            );
        }

        desktop.open(normalizedDirectory.toFile());
    }
}
