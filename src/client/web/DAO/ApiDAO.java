package web.DAO;
import core.*;
import web.DTO.*;
import java.util.*;
/** Browser -> Client Java (with built-in gateway) -> team's APIs. Cookies stay on Java server. */
public final class ApiDAO {
    private final Settings s;
    public ApiDAO(Settings s) {
        this.s=s;
    }
    public Object call(ClientSessionDTO session,String method,String path,Object body)throws Exception {
        if(!path.startsWith("/api/"))throw new Http.Error(400,"Invalid API path");
        return Http.call(method,s.gateway()+path,session.upstreamCookie,body).require().json();
    }
    public List<Map<String,Object>> pois(ClientSessionDTO ss,boolean admin)throws Exception {
        return Data.list(call(ss,"GET",admin?"/api/admin/pois":"/api/pois",null));
    }
    public List<Map<String,Object>> areas(ClientSessionDTO ss)throws Exception {
        return Data.list(call(ss,"GET","/api/campus-areas",null));
    }
    public List<Map<String,Object>> languages(ClientSessionDTO ss)throws Exception {
        return Data.list(call(ss,"GET","/api/languages",null));
    }
    public List<Map<String,Object>> contents(ClientSessionDTO ss,boolean admin)throws Exception {
        return Data.list(call(ss,"GET",admin?"/api/admin/narrations":"/api/narrations",null));
    }
    public List<Map<String,Object>> audio(ClientSessionDTO ss,long id,boolean admin)throws Exception {
        return Data.list(call(ss,"GET","/api/audio?contentId="+id+(admin?"&admin=1":""),null));
    }
    public void login(ClientSessionDTO ss, String username, String password) throws Exception {
    Http.Reply r = Http.call(
        "POST",
        s.gateway() + "/api/auth/login",
        "",
        Data.row("username", username, "password", password)
    ).require();

    String h = r.header("Set-Cookie");

    String cookie = Arrays.stream(h.split(";"))
        .map(String::trim)
        .filter(v -> v.startsWith("SGU_ADMIN_SESSION="))
        .findFirst()
        .orElse("");

    if (!cookie.matches("SGU_ADMIN_SESSION=[A-Za-z0-9._-]{1,256}")) {
        throw new Http.Error(502, "Auth service chưa trả cookie SGU_ADMIN_SESSION.");
    }

    ss.upstreamCookie = cookie;
    ss.admin = Data.map(Data.map(r.json()).get("admin"));
}
    public void me(ClientSessionDTO ss)throws Exception {
        if(!ss.loggedIn())throw new Http.Error(401,"Vui lòng đăng nhập.");
        try {
            ss.admin=Data.map(call(ss,"GET","/api/auth/me",null));
        }
        catch(Http.Error e) {
            if(e.status==401) {
                ss.admin=null;
                ss.upstreamCookie="";
            }
            throw e;
        }
    }
    public void logout(ClientSessionDTO ss)throws Exception {
        String cookie=ss.upstreamCookie;
        try {
            if(!cookie.isEmpty())Http.call("POST",s.gateway()+"/api/auth/logout",cookie,Map.of()).require();
        }
        finally {
            ss.admin=null;
            ss.upstreamCookie="";
        }
    }
    public long savePoi(ClientSessionDTO ss,long id,Map<String,Object> dto)throws Exception {
        Object v=call(ss,id==0?"POST":"PUT","/api/admin/pois"+(id==0?"":"/"+id),dto);
        return id==0?Data.id(Data.map(v),"id"):id;
    }
    public void visible(ClientSessionDTO ss,long id,boolean visible)throws Exception {
        call(ss,"PATCH","/api/admin/pois/"+id+"/visibility",Data.row("visible",visible));
    }
    public long saveContent(ClientSessionDTO ss,long id,Map<String,Object> dto)throws Exception {
        return Data.id(Data.map(call(ss,id==0?"POST":"PUT","/api/admin/narrations"+(id==0?"":"/"+id),dto)),"id");
    }
    public Map<String,Object> translateContent(
            ClientSessionDTO ss,
            String sourceLanguage,
            String targetLanguage,
            String title,
            String content
    ) throws Exception {
        return Data.map(call(
                ss,
                "POST",
                "/api/admin/narrations/translate",
                Data.row(
                        "sourceLanguage", sourceLanguage,
                        "targetLanguage", targetLanguage,
                        "title", title,
                        "content", content
                )
        ));
    }
    public Map<String,Object> startAutoNarration(
            ClientSessionDTO ss,
            Map<String,Object> request
    ) throws Exception {
        return Data.map(call(
                ss, "POST", "/api/admin/narrations/auto/jobs", request
        ));
    }

    public Map<String,Object> autoNarrationProgress(
            ClientSessionDTO ss,
            String jobId
    ) throws Exception {
        if(jobId == null
                || !jobId.matches(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                )) {
            throw new Http.Error(400,"Mã tác vụ không hợp lệ.");
        }

        return Data.map(call(
                ss, "GET",
                "/api/admin/narrations/auto/jobs/" + jobId,
                null
        ));
    }
    public void upload(ClientSessionDTO ss,long id,String filename,byte[] file)throws Exception {
        Http.request("POST",s.gateway()+"/api/admin/audio?contentId="+id+"&filename="+Http.enc(filename),ss.upstreamCookie,"audio/mpeg",file,Map.of()).require();
    }
    public Http.Reply media(ClientSessionDTO ss,long id,boolean preview,String method,String range)throws Exception {
        Map<String,String> extra=range==null?Map.of():Map.of("Range",range);
        return Http.request(method,s.gateway()+"/api/audio/"+id+"/stream"+(preview?"?admin=1":""),preview?ss.upstreamCookie:"",null,null,extra);
    }
    public List<Map<String,Object>> health() {
        List<Map<String,Object>> rows=new ArrayList<>();
        Map<String,String> targets=new LinkedHashMap<>();
        targets.put("Client API", s.gateway() + "/health");
targets.put("narration-server", s.narration() + "/api/health");
targets.put("audio-server", s.audio() + "/api/health");
        targets.put("poi-service",s.gateway()+"/api/pois");
        for(var e:targets.entrySet()) {
            try {
                Http.Reply r=Http.call("GET",e.getValue(),"",null);
                rows.add(Data.row("service",e.getKey(),"status",r.status()==200?"OK":"HTTP "+r.status(),"url",e.getValue()));
            }
            catch(Exception ex) {
                rows.add(Data.row("service",e.getKey(),"status","Chưa kết nối","url",e.getValue()));
            }
        }
        return rows;
    }
}
