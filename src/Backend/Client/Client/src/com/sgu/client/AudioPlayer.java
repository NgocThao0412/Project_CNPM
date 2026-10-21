package com.sgu.client;

import javax.sound.sampled.*;
import java.awt.Desktop;
import java.net.*;
import java.nio.file.*;

/** WAV có bộ điều khiển trong Swing; MP3 được mở bằng ứng dụng phát mặc định của OS. */
public final class AudioPlayer {
    private Clip clip;
    public synchronized String play(String audioUrl) throws Exception {
        stop();
        if(audioUrl==null || audioUrl.isBlank())throw new IllegalArgumentException("POI/ngôn ngữ này chưa có audio.");
        URI url=URI.create(audioUrl);
        String scheme=url.getScheme()==null?"":url.getScheme().toLowerCase(java.util.Locale.ROOT);
        if(!scheme.equals("http")&&!scheme.equals("https")&&!scheme.equals("file"))
            throw new IllegalArgumentException("URL audio phải là http, https hoặc file cục bộ.");
        String filename=url.getPath()==null?"":url.getPath().toLowerCase(java.util.Locale.ROOT);
        if(!(filename.endsWith(".wav")||filename.endsWith(".mp3")||filename.endsWith(".ogg")||filename.endsWith(".m4a")))
            throw new IllegalArgumentException("Đường dẫn audio cần kết thúc bằng định dạng tệp âm thanh.");
        boolean wav=filename.endsWith(".wav");
        if(wav){
            try(AudioInputStream audio=AudioSystem.getAudioInputStream(url.toURL())){
                clip=AudioSystem.getClip();clip.open(audio);clip.start();
                return "Đang phát WAV trong ứng dụng Java.";
            }
        }
        if(!Desktop.isDesktopSupported())throw new UnsupportedOperationException("Hệ điều hành không hỗ trợ mở MP3. Dùng trình phát MP3 ngoài hoặc cung cấp file WAV.");
        Desktop desktop=Desktop.getDesktop();
        if("file".equalsIgnoreCase(url.getScheme())) desktop.open(Path.of(url).toFile());
        else desktop.browse(url);
        return "MP3 đã gửi cho ứng dụng phát mặc định của hệ điều hành (không thể điều khiển dừng từ Swing).";
    }
    public synchronized void stop(){if(clip!=null){clip.stop();clip.close();clip=null;}}
}
