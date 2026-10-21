package com.sgu.client;

import java.util.*;

/** Cac model UI doc lap voi Java Backend DTO (co adapter POIDTO). */
public final class Models {
    private Models() {}

    public record Admin(long id, String username, String fullName, String email) {}
    public record Poi(long id, String code, String name, long areaId, String area,
                      String description, double latitude, double longitude,
                      double radiusM, String imageUrl, boolean visible) {
        public String toString() { return code + " - " + name; }
    }
    public record Narration(long id, long poiId, String languageCode, String title,
                            String content, String audioUrl, boolean visible) {
        public String toString() { return title + " (" + languageCode + ")"; }
    }

    public static final List<String> LANGS = List.of("vi", "en", "fr");
    public static String string(Object value) { return value == null ? "" : String.valueOf(value); }
    public static long lng(Object x) {
        try { return x == null ? 0 : Long.parseLong(String.valueOf(x)); }
        catch (NumberFormatException e) { return 0; }
    }
    public static double dbl(Object x, double defaultValue) {
        try { return x == null ? defaultValue : Double.parseDouble(String.valueOf(x)); }
        catch (NumberFormatException e) { return defaultValue; }
    }
    public static boolean bool(Object x, boolean def) {
        return x == null ? def : (x.equals(Boolean.TRUE) || "1".equals(String.valueOf(x)) || "true".equalsIgnoreCase(String.valueOf(x)));
    }
    public static Object first(Map<String,Object> m, String... keys) {
        for (String k : keys) if (m.containsKey(k)) return m.get(k);
        return null;
    }
    public static Poi parsePoi(Map<String,Object> o) {
        return new Poi(lng(o.get("id")), string(first(o,"poiCode","code")),
                string(first(o,"poiName","name")), lng(first(o,"areaId","area_id")),
                string(first(o,"areaName","area")), string(o.get("description")),
                dbl(o.get("latitude"),0), dbl(o.get("longitude"),0),
                dbl(first(o,"triggerRadiusM","radius_m"),50),
                string(first(o,"imageUrl","image_url")), bool(first(o,"isVisible","visible"),true));
    }
    public static Narration parseNarration(Map<String,Object> o) {
        return new Narration(lng(o.get("id")), lng(first(o,"poiId","poi_id")),
                string(first(o,"languageCode","language_code")),
                string(o.get("title")), string(first(o,"content","body")),
                string(first(o,"audioUrl","audio_url")), bool(first(o,"isVisible","visible"),true));
    }
    public static Map<String,Object> poiBody(Poi p) {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("poiCode",p.code()); data.put("poiName",p.name());
        data.put("areaId",p.areaId() == 0 ? null : p.areaId());
        data.put("description",p.description()); data.put("latitude",p.latitude());
        data.put("longitude",p.longitude()); data.put("triggerRadiusM",p.radiusM());
        data.put("imageUrl",p.imageUrl()); data.put("isVisible",p.visible());
        return data;
    }
    public static Map<String,Object> narrationBody(Narration n) {
        return Map.of("poiId",n.poiId(),"languageCode",n.languageCode(),
                "title",n.title(),"content",n.content(),"isVisible",n.visible());
    }
    public static void validatePoi(Poi p) {
        if (p.code().isBlank() || p.name().isBlank()) throw new IllegalArgumentException("Mã và tên POI không được trống.");
        if (!Double.isFinite(p.latitude()) || Math.abs(p.latitude()) > 90 ||
            !Double.isFinite(p.longitude()) || Math.abs(p.longitude()) > 180 ||
            !Double.isFinite(p.radiusM()) || p.radiusM() <= 0)
            throw new IllegalArgumentException("Tọa độ hoặc bán kính GPS không hợp lệ.");
    }
    public static void validateNarration(Narration n) {
        if (n.poiId() < 1 || n.languageCode().isBlank() || n.title().isBlank() || n.content().isBlank())
            throw new IllegalArgumentException("Cần chọn POI, ngôn ngữ, tiêu đề và nhập nội dung.");
    }
}
