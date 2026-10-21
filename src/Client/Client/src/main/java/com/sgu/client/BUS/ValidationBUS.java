package com.sgu.client.BUS;

import com.sgu.client.DTO.*;

/** Kiem tra quy tac nghiep vu POI va thuyet minh. */
public final class ValidationBUS {
    private ValidationBUS() {}
    public static void validatePoi(POIDTO p) {
        if (p.code().isBlank() || p.name().isBlank()) throw new IllegalArgumentException("Mã và tên POI không được trống.");
        if (!Double.isFinite(p.latitude()) || Math.abs(p.latitude()) > 90 ||
            !Double.isFinite(p.longitude()) || Math.abs(p.longitude()) > 180 ||
            !Double.isFinite(p.radiusM()) || p.radiusM() <= 0)
            throw new IllegalArgumentException("Tọa độ hoặc bán kính GPS không hợp lệ.");
    }
    public static void validateNarration(NarrationDTO n) {
        if (n.poiId() < 1 || n.languageCode().isBlank() || n.title().isBlank() || n.content().isBlank())
            throw new IllegalArgumentException("Cần chọn POI, ngôn ngữ, tiêu đề và nhập nội dung.");
    }
}
