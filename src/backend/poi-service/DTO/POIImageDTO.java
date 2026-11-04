package DTO;

/** Metadata for an image belonging to a POI; file bytes are stored on disk. */
public record POIImageDTO(
        Long id, Long poiId, String fileUrl, String caption, int sortOrder
) {}
