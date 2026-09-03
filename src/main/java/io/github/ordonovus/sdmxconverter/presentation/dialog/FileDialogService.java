package io.github.ordonovus.sdmxconverter.presentation.dialog;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Provides the native file and directory dialogs used by the application.
 */
public final class FileDialogService {

    /**
     * Opens a dialog that allows selecting multiple Excel files.
     *
     * @param owner owner window of the dialog
     * @param initialLocation preferred initial file or directory
     * @return normalized paths selected by the user
     */
    public List<Path> chooseExcelFiles(
            Window owner,
            Path initialLocation
    ) {
        var chooser = createFileChooser(
                "Seleccionar archivos Excel",
                initialLocation,
                List.of(
                        new FileChooser.ExtensionFilter(
                                "Archivos Excel (*.xls, *.xlsx)",
                                "*.xls",
                                "*.xlsx"
                        )
                )
        );

        List<File> selectedFiles = chooser.showOpenMultipleDialog(owner);

        if (selectedFiles == null) return List.of();

        return selectedFiles.stream()
                .map(File::toPath)
                .map(FileDialogService::normalize)
                .toList();
    }

    /**
     * Opens a dialog for selecting an SDMX DSD XML file.
     *
     * @param owner owner window of the dialog
     * @param initialLocation preferred initial file or directory
     * @return selected normalized path, or empty when cancelled
     */
    public Optional<Path> chooseDsdFile(
            Window owner,
            Path initialLocation
    ) {
        return chooseSingleFile(
                owner,
                "Seleccionar archivo DSD",
                initialLocation,
                List.of(
                        new FileChooser.ExtensionFilter(
                                "Archivos XML (*.xml)",
                                "*.xml"
                        ),
                        new FileChooser.ExtensionFilter(
                                "Todos los archivos",
                                "*.*"
                        )
                )
        );
    }

    /**
     * Opens a dialog for selecting an SDMX header properties file.
     *
     * @param owner owner window of the dialog
     * @param initialLocation preferred initial file or directory
     * @return selected normalized path, or empty when cancelled
     */
    public Optional<Path> chooseHeaderFile(
            Window owner,
            Path initialLocation
    ) {
        return chooseSingleFile(
                owner,
                "Seleccionar archivo de encabezado",
                initialLocation,
                List.of(
                        new FileChooser.ExtensionFilter(
                                "Archivos de propiedades (*.prop, *.properties)",
                                "*.prop",
                                "*.properties"
                        ),
                        new FileChooser.ExtensionFilter(
                                "Todos los archivos",
                                "*.*"
                        )
                )
        );
    }

    /**
     * Opens a dialog for selecting the XML output directory.
     *
     * @param owner owner window of the dialog
     * @param initialLocation preferred initial file or directory
     * @return selected normalized directory, or empty when cancelled
     */
    public Optional<Path> chooseOutputDirectory(
            Window owner,
            Path initialLocation
    ) {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta de salida");

        File initialDirectory = resolveInitialDirectory(initialLocation);

        if (initialDirectory != null) {
            chooser.setInitialDirectory(initialDirectory);
        }

        File selectedDirectory = chooser.showDialog(owner);

        return selectedDirectory == null
                ? Optional.empty()
                : Optional.of(normalize(selectedDirectory.toPath()));
    }

    private Optional<Path> chooseSingleFile(
            Window owner,
            String title,
            Path initialLocation,
            List<FileChooser.ExtensionFilter> extensionFilters
    ) {
        var chooser = createFileChooser(
                title,
                initialLocation,
                extensionFilters
        );

        File selectedFile = chooser.showOpenDialog(owner);

        return selectedFile == null
                ? Optional.empty()
                : Optional.of(normalize(selectedFile.toPath()));
    }

    private FileChooser createFileChooser(
            String title,
            Path initialLocation,
            List<FileChooser.ExtensionFilter> extensionFilters
    ) {
        var chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(extensionFilters);

        File initialDirectory = resolveInitialDirectory(initialLocation);

        if (initialDirectory != null) {
            chooser.setInitialDirectory(initialDirectory);
        }

        return chooser;
    }

    private File resolveInitialDirectory(Path location) {
        if (location == null) {
            return null;
        }

        Path normalizedLocation = normalize(location);

        Path directory = Files.isDirectory(normalizedLocation)
                ? normalizedLocation
                : normalizedLocation.getParent();

        return directory != null && Files.isDirectory(directory)
                ? directory.toFile()
                : null;
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

}
