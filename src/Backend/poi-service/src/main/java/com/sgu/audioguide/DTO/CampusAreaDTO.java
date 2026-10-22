package com.sgu.audioguide.DTO;

public record CampusAreaDTO(
        Long id,
        String areaCode,
        String areaName,
        Boolean visible
) {
}