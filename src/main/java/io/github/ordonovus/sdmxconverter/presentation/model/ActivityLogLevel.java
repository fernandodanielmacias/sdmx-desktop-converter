package io.github.ordonovus.sdmxconverter.presentation.model;

/**
 * Defines the severity levels supported by the activity log.
 */
public enum ActivityLogLevel {

    INFORMATION("Información", "activity-information"),
    PROCESSING("Procesando", "activity-processing"),
    SUCCESS("Correcto", "activity-success"),
    WARNING("Advertencia", "activity-warning"),
    ERROR("Error", "activity-error");

    private final String displayName;
    private final String styleClass;

    /**
     * Creates an activity log level.
     *
     * @param displayName level name displayed to the user
     * @param styleClass CSS class used to represent the level
     */
    ActivityLogLevel(
            String displayName,
            String styleClass
    ) {
        this.displayName = displayName;
        this.styleClass = styleClass;
    }

    /**
     * Returns the localized name displayed in the activity log.
     *
     * @return user-visible level name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the CSS class associated with this level.
     *
     * @return activity log CSS class
     */
    public String getStyleClass() {
        return styleClass;
    }
}