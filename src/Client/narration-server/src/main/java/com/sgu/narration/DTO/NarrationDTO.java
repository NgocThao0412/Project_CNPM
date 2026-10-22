package com.sgu.narration.DTO;
/** Uses the exact narration_contents / languages relationships in SGU_audio_guide.sql. */
public record NarrationDTO(long id,long poiId,long languageId,String languageCode,String languageName,String title,String content,boolean visible) {
}
