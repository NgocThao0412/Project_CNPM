package com.sgu.audio;
import com.sgu.core.*;
import com.sgu.audio.controller.*;
public class Application {
    public static void main(String[] args)throws Exception {
        Settings s=new Settings();
        AudioController c=new AudioController(s);
        int port=s.integer("audio.port",8084);
        Http.serve(s.get("services.bind","127.0.0.1"),port,c::handle);
        System.out.println("audio-server :"+port+" mode="+s.mode());
    }
}
