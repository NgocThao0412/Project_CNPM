package com.sgu.client.DTO;

import java.util.*;

/** Chuyen doi JSON tu cac Java microservices sang DTO Client. */
public final class DtoMapper {
    private DtoMapper() {}
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
    public static POIDTO parsePoi(Map<String,Object> o) {
        return new POIDTO(lng(o.get("id")), string(first(o,"poiCode","code")),
                string(first(o,"poiName","name")), lng(first(o,"areaId","area_id")),
                string(first(o,"areaName","area")), string(o.get("description")),
                dbl(o.get("latitude"),0), dbl(o.get("longitude"),0),
                dbl(first(o,"triggerRadiusM","radius_m"),50),
                string(first(o,"imageUrl","image_url")), bool(first(o,"isVisible","visible"),true));
    }
    public static NarrationDTO parseNarration(Map<String,Object> o) {
        return new NarrationDTO(lng(o.get("id")), lng(first(o,"poiId","poi_id")),
                string(first(o,"languageCode","language_code")),
                string(o.get("title")), string(first(o,"content","body")),
                string(first(o,"audioUrl","audio_url")), bool(first(o,"isVisible","visible"),true));
    }
    public static Map<String,Object> poiBody(POIDTO p) {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("poiCode",p.code()); data.put("poiName",p.name());
        data.put("areaId",p.areaId() == 0 ? null : p.areaId());
        data.put("description",p.description()); data.put("latitude",p.latitude());
        data.put("longitude",p.longitude()); data.put("triggerRadiusM",p.radiusM());
        data.put("imageUrl",p.imageUrl()); data.put("isVisible",p.visible());
        return data;
    }
    public static Map<String,Object> narrationBody(NarrationDTO n) {
        return Map.of("poiId",n.poiId(),"languageCode",n.languageCode(),
                "title",n.title(),"content",n.content(),"isVisible",n.visible());
    }
}
