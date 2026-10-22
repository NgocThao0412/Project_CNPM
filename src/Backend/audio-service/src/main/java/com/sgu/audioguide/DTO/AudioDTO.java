package com.sgu.audioguide.DTO;
public record AudioDTO(Long id,Long contentId,String fileName,String fileUrl,Long fileSizeBytes,
                       Double durationSeconds,int versionNumber,Boolean isVisible) {}
