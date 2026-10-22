package com.sgu.audioguide.controller;
import com.sgu.audioguide.BUS.AudioBUS;
import com.sgu.audioguide.DTO.AudioDTO;
import com.sgu.audioguide.config.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
  private final AudioBUS bus; private final AdminAccess access;
  public AudioController(AudioBUS bus,AdminAccess access){this.bus=bus;this.access=access;}
  @PostMapping(value="/narrations/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public AudioDTO upload(@PathVariable long id,@RequestPart("audio") MultipartFile file,
                         @RequestParam("durationSeconds") double durationSeconds,HttpServletRequest r)throws IOException{
     access.requireAdmin(r);return bus.upload(id,file,durationSeconds);
  }
  @GetMapping(value="/files/{id}.mp3",produces="audio/mpeg")
  public ResponseEntity<Resource> download(@PathVariable long id){return ResponseEntity.ok()
     .contentType(MediaType.parseMediaType("audio/mpeg")).body(bus.open(id));}
}
