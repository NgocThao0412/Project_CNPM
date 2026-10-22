package com.sgu.gateway.controller;
import com.sgu.core.*;
import com.sgu.gateway.BUS.*;
import com.sgu.gateway.DAO.*;
import com.sun.net.httpserver.*;
import java.util.*;
public final class GatewayController {
    private final Settings s;
    private final DemoAuthBUS auth;
    private final DemoCatalogDAO catalog;
    private final UpstreamDAO upstream;
    public GatewayController(Settings s)throws Exception {
        this.s=s;
        auth=s.demo()?new DemoAuthBUS():null;
        catalog=s.demo()?new DemoCatalogDAO(s):null;
        upstream=new UpstreamDAO(s);
    }
    public void handle(HttpExchange x)throws Exception {
        String p=x.getRequestURI().getPath(),method=x.getRequestMethod();
        if(p.equals("/health")) {
            Http.json(x,200,Data.row("service","client-gateway","mode",s.mode(),"status","UP"));
            return;
        } if(s.demo()) {
            if(p.equals("/api/auth/login")&&method.equals("POST")) {
                String t=auth.login(Http.jsonBody(x),x.getRemoteAddress().getAddress().getHostAddress());
                x.getResponseHeaders().add("Set-Cookie","SGU_ADMIN_SESSION="+t+"; Path=/; HttpOnly; SameSite=Lax");
                Http.json(x,200,Data.row("message","Đăng nhập thành công","admin",auth.me(t)));
                return;
            } if(p.equals("/api/auth/me")&&method.equals("GET")) {
                Http.json(x,200,auth.me(Http.cookie(x,"SGU_ADMIN_SESSION")));
                return;
            } if(p.equals("/api/auth/logout")&&method.equals("POST")) {
                auth.logout(Http.cookie(x,"SGU_ADMIN_SESSION"));
                Http.json(x,200,Data.row("message","Đã đăng xuất"));
                return;
            } if(p.equals("/api/campus-areas")&&method.equals("GET")) {
                Http.json(x,200,catalog.areas());
                return;
            } if(p.equals("/api/pois")&&method.equals("GET")) {
                Http.json(x,200,catalog.list(false));
                return;
            } if(p.startsWith("/api/admin/pois")) {
                auth.me(Http.cookie(x,"SGU_ADMIN_SESSION"));
                if(p.equals("/api/admin/pois")&&method.equals("GET")) {
                    Http.json(x,200,catalog.list(true));
                    return;
                } if(p.equals("/api/admin/pois")&&method.equals("POST")) {
                    long id=catalog.save(0,POIBUS.validate(Http.jsonBody(x)));
                    Http.json(x,201,Data.row("id",id,"message","Đã thêm POI"));
                    return;
                } if(p.matches("/api/admin/pois/[0-9]+")&&method.equals("PUT")) {
                    long id=Long.parseLong(p.substring(p.lastIndexOf('/')+1));
                    catalog.save(id,POIBUS.validate(Http.jsonBody(x)));
                    Http.json(x,200,Data.row("id",id));
                    return;
                } if(p.matches("/api/admin/pois/[0-9]+/visibility")&&method.equals("PATCH")) {
                    long id=Long.parseLong(p.split("/")[4]);
                    catalog.visibility(id,Data.bool(Http.jsonBody(x),"visible",false));
                    Http.json(x,200,Data.row("id",id));
                    return;
                }
            }
        } upstream.forward(x);
    }
}
