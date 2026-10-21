package com.sgu.audioguide.DTO;
public record NarrationDTO(Long id,Long poiId,String languageCode,String title,String content,String audioUrl,Boolean isVisible) {}
