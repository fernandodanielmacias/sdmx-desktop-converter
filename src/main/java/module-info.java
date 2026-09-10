module io.github.ordonovus.sdmxconverter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.desktop;

    exports io.github.ordonovus.sdmxconverter;

    opens io.github.ordonovus.sdmxconverter.presentation.controller
            to javafx.fxml;
}