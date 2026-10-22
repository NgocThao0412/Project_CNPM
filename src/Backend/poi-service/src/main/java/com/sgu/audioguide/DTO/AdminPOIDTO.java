package com.sgu.audioguide.DTO;

import java.math.BigDecimal;

public record AdminPOIDTO(
        Long id,
        String poiCode,
        String poiName,
        Long areaId,
        String areaName,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal triggerRadiusM,
        String imageUrl,
        Boolean visible
) {
}