package com.sgu.audioguide.DTO;
import java.math.BigDecimal;
/** Additive DTO. The original public POIDTO and CreatePOIRequestDTO remain unchanged. */
public record POIManagementDTO(Long id,String poiCode,String poiName,Long areaId,String areaName,String description,BigDecimal latitude,BigDecimal longitude,BigDecimal triggerRadiusM,String imageUrl,boolean visible) {
}
