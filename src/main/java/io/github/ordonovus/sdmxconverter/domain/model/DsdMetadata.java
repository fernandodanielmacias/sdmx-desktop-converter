package io.github.ordonovus.sdmxconverter.domain.model;

import java.util.Objects;

/**
 * Contains the identity information extracted from an SDMX Data Structure
 * Definition.
 *
 * @param agencyId organization responsible for maintaining the DSD
 * @param id unique identifier of the DSD
 * @param version version assigned to the DSD
 */
public record DsdMetadata(
        String agencyId,
        String id,
        String version
) {

    /**
     * Creates validated DSD metadata.
     */
    public DsdMetadata {
        agencyId = requireText(agencyId, "DSD agency ID");
        id = requireText(id, "DSD ID");
        version = requireText(version, "DSD version");
    }

    /**
     * Returns the complete SDMX identity using the agency, ID and version.
     *
     * @return formatted identity such as {@code ESTAT:NA_MAIN(1.17.0)}
     */
    public String formattedIdentity() {
        return "%s:%s(%s)".formatted(agencyId, id, version);
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

}
