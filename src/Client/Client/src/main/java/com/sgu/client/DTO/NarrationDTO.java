package com.sgu.client.DTO;

/** Du lieu truyen qua cac tang, khong phu thuoc Java Swing. */
public record NarrationDTO(long id, long poiId, String languageCode, String title,
                            String content, String audioUrl, boolean visible) {
    @Override public String toString() { return title + " (" + languageCode + ")"; }
}
