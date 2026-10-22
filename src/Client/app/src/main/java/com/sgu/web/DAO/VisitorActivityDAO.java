package com.sgu.web.DAO;

import com.sgu.core.Data;
import com.sgu.core.Http;
import com.sgu.core.Settings;
import com.sgu.web.BUS.VisitorActivityBUS;
import com.sgu.web.DTO.ClientSessionDTO;
import java.util.Map;

/** Only the Client process communicates with visitor-service (port 8085). */
public final class VisitorActivityDAO {
    private final Settings settings;
    private final VisitorActivityBUS demo=new VisitorActivityBUS();
    public VisitorActivityDAO(Settings settings) { this.settings=settings; }
    private String endpoint(String path) {
        return settings.get("visitor.url","http://127.0.0.1:8085")+path;
    }
    public String create() throws Exception {
        if(settings.demo())return demo.create();
        Map<String,Object> response=Data.map(Http.call("POST",endpoint("/api/visitor/sessions"),"",null).require().json());
        return checkedToken(Data.text(response,"token"));
    }
    public void heartbeat(String token,boolean listening) throws Exception {
        token=checkedToken(token);
        if(settings.demo()) { demo.heartbeat(token,listening); return; }
        Http.request("POST",endpoint("/api/visitor/heartbeat"),"",
                "application/json",com.sgu.core.Json.stringify(Data.row("listening",listening))
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Map.of("X-Visitor-Token",token)).require();
    }
    public void end(String token) throws Exception {
        token=checkedToken(token);
        if(settings.demo()) { demo.end(token); return; }
        Http.request("POST",endpoint("/api/visitor/end"),"",null,null,
                Map.of("X-Visitor-Token",token)).require();
    }
    public Map<String,Object> stats(ClientSessionDTO admin) throws Exception {
        Map<String,Object> response;
        if(settings.demo())response=demo.stats();
        else {
            if(admin==null || !admin.loggedIn())throw new Http.Error(401,"Vui lòng đăng nhập quản trị.");
            // The existing authentication cookie is forwarded server-to-server, not exposed to JS.
            response=Data.map(Http.call("GET",endpoint("/api/admin/visitor-stats"),
                    admin.upstreamCookie,null).require().json());
        }
        int visitors=nonnegative(response.get("activeVisitors"),"activeVisitors");
        int listeners=nonnegative(response.get("activeListeners"),"activeListeners");
        if(listeners>visitors)throw new Http.Error(502,"Số phiên đang nghe vượt tổng phiên truy cập.");
        return Data.row("activeVisitors",visitors,"activeListeners",listeners);
    }
    private static int nonnegative(Object value,String field) {
        if(!(value instanceof Number n) || n.longValue()!=n.doubleValue() || n.longValue()<0
                || n.longValue()>Integer.MAX_VALUE)
            throw new Http.Error(502,"Phản hồi visitor-service thiếu/sai trường "+field);
        return n.intValue();
    }
    public static String checkedToken(String token) {
        if(token==null || !token.matches("[A-Za-z0-9._~-]{8,512}"))
            throw new Http.Error(400,"Visitor token không hợp lệ.");
        return token;
    }
}
