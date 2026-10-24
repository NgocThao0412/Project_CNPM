package DTO;

import java.math.BigDecimal;

public record POIDTO(
        Long id,
        String poiCode,
        String poiName,
        Long areaId,
        String areaName,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal triggerRadiusM,
        String imageUrl
) {
}