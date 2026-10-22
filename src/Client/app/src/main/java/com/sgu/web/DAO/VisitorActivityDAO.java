package com.sgu.web.DAO;

import com.sgu.core.Data;
import com.sgu.core.Http;
import com.sgu.core.Settings;
import com.sgu.web.BUS.VisitorActivityBUS;
import com.sgu.web.DTO.ClientSessionDTO;
import java.util.Map;

/** LIVE: Client forwards requests to an independently implemented visitor-service. */
public final class VisitorActivityDAO {
    private final Settings settings;
    private final VisitorActivityBUS demo = new VisitorActivityBUS();
    public VisitorActivityDAO(Settings settings) { this.settings=settings; }

    private String endpoint(String path) {
        return settings.get("visitor.url","http://127.0.0.1:8086")+path;
    }
    public void activity(Map<String,Object> event)throws Exception {
        var checked=VisitorActivityBUS.validate(event);
        if(settings.demo()) { demo.accept(checked); return; }
        Http.call("POST",endpoint("/api/visitor/activity"),"",checked).require();
    }
    public Map<String,Object> online(ClientSessionDTO admin)throws Exception {
        if(settings.demo()) return demo.summary();
        // The API owner must validate auth-service's SGU_ADMIN_SESSION cookie.
        Map<String,Object> response=Data.map(Http.call("GET",
                endpoint("/api/admin/visitors/online"),admin.upstreamCookie,null).require().json());
        if(!response.containsKey("online") || !response.containsKey("listening")
                || !response.containsKey("visitors"))
            throw new Http.Error(502,"API visitor-service chưa đúng hợp đồng dữ liệu.");
        return response;
    }
}
