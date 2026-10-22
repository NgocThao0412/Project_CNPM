package com.sgu.narration.controller;
import com.sgu.core.*;
import com.sgu.narration.DAO.*;
import com.sgu.narration.BUS.*;
import com.sun.net.httpserver.*;
import java.util.*;
public final class NarrationController {
    private final Settings s;
    private final NarrationDAO dao;
    private final NarrationBUS bus;
    public NarrationController(Settings s)throws Exception {
        this.s=s;
        dao=new NarrationDAO(s);
        bus=new NarrationBUS(dao);
    }
    public void handle(HttpExchange x)throws Exception {
        String p=x.getRequestURI().getPath(),method=x.getRequestMethod(),cookie=Http.authCookie(x);
        boolean admin=p.startsWith("/api/admin/");
        if(p.equals("/health")) {
            Http.json(x,200,Data.row("service","narration-server","mode",s.mode(),"status","UP"));
            return;
        }
        if(admin)Http.requireAdmin(s,cookie);
        if(p.equals("/api/languages")&&method.equals("GET")) {
            Http.json(x,200,dao.languages());
            return;
        } if((p.equals("/api/narrations")||p.equals("/api/admin/narrations"))&&method.equals("GET")) {
            var q=Http.query(x);
            List<Map<String,Object>> list=dao.list(admin);
            if(q.containsKey("poiId")) {
                long id=Long.parseLong(q.get("poiId"));
                list=list.stream().filter(m->Data.id(m,"poiId")==id).toList();
            }
            if(q.containsKey("languageCode")) {
                String code=q.get("languageCode");
                list=list.stream().filter(m->Data.text(m,"languageCode").equals(code)).toList();
            }
            Http.json(x,200,list);
            return;
        } if(p.matches("/api/(admin/)?narrations/[0-9]+")&&method.equals("GET")) {
            Http.json(x,200,dao.get(Long.parseLong(p.substring(p.lastIndexOf('/')+1)),admin));
            return;
        } if(p.equals("/api/admin/narrations")&&method.equals("POST")) {
            Http.json(x,201,Data.row("id",bus.save(0,Http.jsonBody(x),cookie)));
            return;
        } if(p.matches("/api/admin/narrations/[0-9]+")&&method.equals("PUT")) {
            Http.json(x,200,Data.row("id",bus.save(Long.parseLong(p.substring(p.lastIndexOf('/')+1)),Http.jsonBody(x),cookie)));
            return;
        } throw new Http.Error(404,"Narration endpoint not found");
    }
}
