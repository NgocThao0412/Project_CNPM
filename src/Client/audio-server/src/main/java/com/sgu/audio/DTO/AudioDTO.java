package com.sgu.audio.DTO;
public record AudioDTO(long id,long contentId,String fileName,String audioUrl,double durationSeconds,int versionNumber,boolean visible) {
}
