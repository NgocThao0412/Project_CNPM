package com.sgu.audio.controller;
import com.sgu.core.*;
import com.sgu.audio.DAO.*;
import com.sgu.audio.BUS.*;
import com.sun.net.httpserver.*;
public final class AudioController {
    private final Settings s;
    private final AudioDAO dao;
    private final AudioBUS bus;
    public AudioController(Settings s)throws Exception {
        this.s=s;
        dao=new AudioDAO(s);
        bus=new AudioBUS(dao);
    }
    public void handle(HttpExchange x)throws Exception {
        String p=x.getRequestURI().getPath(),method=x.getRequestMethod(),cookie=Http.authCookie(x);
        var q=Http.query(x);
        boolean admin=p.startsWith("/api/admin/")||"1".equals(q.get("admin"));
        if(p.equals("/health")) {
            Http.json(x,200,Data.row("service","audio-server","mode",s.mode(),"status","UP"));
            return;
        }
        if(admin)Http.requireAdmin(s,cookie);
        if(p.equals("/api/audio")&&method.equals("GET")) {
            long id=Long.parseLong(q.getOrDefault("contentId","0"));
            if(id<=0)throw new Http.Error(400,"Missing contentId");
            Http.json(x,200,dao.list(id,admin));
            return;
        } if(p.matches("/api/audio/[0-9]+/stream")&&(method.equals("GET")||method.equals("HEAD"))) {
            long id=Long.parseLong(p.split("/")[3]);
            bus.stream(x,id,admin);
            return;
        } if(p.equals("/api/admin/audio")&&method.equals("POST")) {
            long content=Long.parseLong(q.getOrDefault("contentId","0"));
            Http.json(x,201,dao.upload(content,q.getOrDefault("filename","audio.mp3"),Http.body(x),cookie));
            return;
        } throw new Http.Error(404,"Audio endpoint not found");
    }
}
