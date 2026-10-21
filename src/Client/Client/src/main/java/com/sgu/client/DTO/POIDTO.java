package com.sgu.client.DTO;

/** Du lieu truyen qua cac tang, khong phu thuoc Java Swing. */
public record POIDTO(long id, String code, String name, long areaId, String area,
                      String description, double latitude, double longitude,
                      double radiusM, String imageUrl, boolean visible) {
    @Override public String toString() { return code + " - " + name; }
}
