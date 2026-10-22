package com.sgu.web.controller;
import com.sgu.core.*;
import com.sgu.web.BUS.*;
import com.sgu.web.DAO.*;
import com.sgu.web.DTO.*;
import com.sgu.web.view.*;
import com.sgu.gateway.controller.GatewayController;
import com.sun.net.httpserver.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
/** Normal HTML forms + Java controllers. No custom JavaScript/Dart/mobile-specific code. */
public final class ClientController {
    private final Settings s;
    private final ApiDAO api;
    private final GatewayController gateway;
    private final Layout layout;
    private final VisitorView visitor;
    private final AdminView admin;
    private final Map<String,ClientSessionDTO> sessions=new ConcurrentHashMap<>();
    public ClientController(Settings s) throws Exception {
        this.s=s;
        gateway=new GatewayController(s);
        api=new ApiDAO(s);
        layout=new Layout(s);
        visitor=new VisitorView(s);
        admin=new AdminView(s);
    } private void setCookie(HttpExchange x,ClientSessionDTO ss) {
        String secure=Boolean.parseBoolean(s.get("web.secure.cookie","false"))?"; Secure":"";
        x.getResponseHeaders().set("Set-Cookie","SGU_CLIENT="+ss.id+"; Path=/; HttpOnly; SameSite=Lax; Max-Age=1800"+secure);
    } private ClientSessionDTO session(HttpExchange x) {
        Instant now=Instant.now();
        sessions.entrySet().removeIf(e->e.getValue().touched.isBefore(now.minusSeconds(1800)));
        ClientSessionDTO ss=sessions.get(Http.cookie(x,"SGU_CLIENT"));
        if(ss==null) {
            if(sessions.size()>10000)throw new Http.Error(503,"Too many sessions");
            ss=new ClientSessionDTO();
            sessions.put(ss.id,ss);
        }
        ss.touched=now;
        setCookie(x,ss);
        return ss;
    } private void csrf(ClientSessionDTO ss,String token) {
        if(token==null||!MessageDigest.isEqual(ss.csrf.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))throw new Http.Error(403,"Biểu mẫu đã hết hạn. Tải lại trang rồi thử lại.");
    } private void page(HttpExchange x,ClientSessionDTO ss,String title,String tab,String body)throws Exception {
        String note=Http.query(x).getOrDefault("notice","");
        if(note.length()>300)note=note.substring(0,300);
        Http.html(x,layout.page(ss,title,tab,body,note));
    } private static long id(Map<String,String> f) {
        try {
            return Long.parseLong(f.getOrDefault("id","0"));
        }
        catch(Exception e) {
            throw new Http.Error(400,"Mã dữ liệu không hợp lệ.");
        }
    } private Map<String,Object> track(ClientSessionDTO ss,Map<String,Object> content,boolean preview)throws Exception {
        if(content==null)return null;
        var a=api.audio(ss,Data.id(content,"id"),preview);
        return a.isEmpty()?null:a.getFirst();
    } private void success(HttpExchange x,String path,String note)throws Exception {
        Http.redirect(x,path+(path.contains("?")?"&":"?")+"notice="+Http.enc(note));
    }
    public void handle(HttpExchange x)throws Exception {
        String p=x.getRequestURI().getPath(),method=x.getRequestMethod();
        // Shared /api routes live in this Client process: no second gateway server.
        if(p.startsWith("/api/")) {
            gateway.handle(x);
            return;
        }
        if(p.startsWith("/assets/")) {
            asset(x,p);
            return;
        }
        if(p.equals("/health")) {
            Http.json(x,200,Data.row("service","client-server","mode",s.mode(),"status","UP"));
            return;
        }
        if(p.equals("/favicon.ico")) {
            x.sendResponseHeaders(204,-1);
            return;
        }
        ClientSessionDTO ss=session(x);
        try {
            route(x,ss,p,method);
        }
        catch(Http.Error e) {
            if(p.startsWith("/media/")) {
                Http.json(x,e.status,Data.row("message",e.getMessage()));
                return;
            }
            if(e.status==401) {
                ss.admin=null;
                ss.upstreamCookie="";
                Http.redirect(x,"/login?notice="+Http.enc("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."));
                return;
            }
            Http.html(x,e.status,layout.page(ss,"Thông báo","","<section class=\"panel error-page\"><span class=\"eyebrow\">SAIGON UNIVERSITY</span><h1>Chưa thể tải nội dung</h1>"+layout.error(e.getMessage())+Html.link("/status","Kiểm tra kết nối","btn")+" "+Html.link("/","Về trang chủ","btn btn-light")+"</section>",""));
        }
    } private void route(HttpExchange x,ClientSessionDTO ss,String p,String method)throws Exception {
        var q=Http.query(x);
        Map<String,String> f=Map.of();
        Map<String,Multipart.Part> parts=Map.of();
        if(method.equals("POST")) {
            String ct=x.getRequestHeaders().getFirst("Content-Type");
            if(ct!=null&&ct.startsWith("multipart/form-data")) {
                parts=Multipart.parse(ct,Http.body(x));
                Map<String,String> v=new LinkedHashMap<>();
                parts.forEach((k,val)-> {
                    if(val.filename().isEmpty())v.put(k,val.text());
                });
                f=v;
            }
            else f=Http.params(new String(Http.body(x),StandardCharsets.UTF_8));
            csrf(ss,f.get("csrf"));
        } if(p.startsWith("/admin")||"1".equals(q.get("preview")))api.me(ss);
        if(p.equals("/login")&&method.equals("GET")) {
            if(ss.loggedIn()) {
                Http.redirect(x,"/admin");
                return;
            }
            page(x,ss,"Đăng nhập","login",admin.login(ss,"",""));
            return;
        } if(p.equals("/login")&&method.equals("POST")) {
            String username=f.getOrDefault("username","");
            try {
                api.login(ss,username,f.getOrDefault("password",""));
                ClientSessionDTO next=new ClientSessionDTO();
                next.admin=ss.admin;
                next.upstreamCookie=ss.upstreamCookie;
                sessions.remove(ss.id);
                sessions.put(next.id,next);
                setCookie(x,next);
                Http.redirect(x,"/admin");
            }
            catch(Http.Error e) {
                page(x,ss,"Đăng nhập","login",admin.login(ss,e.getMessage(),username));
            }
            return;
        } if(p.equals("/logout")&&method.equals("POST")) {
            try {
                api.logout(ss);
            }
            finally {
                sessions.remove(ss.id);
            }
            success(x,"/login","Đã đăng xuất.");
            return;
        } if(p.equals("/")&&method.equals("GET")) {
            page(x,ss,"Khám phá","home",visitor.home(api.pois(ss,false),api.languages(ss)));
            return;
        } if(p.equals("/places")&&method.equals("GET")) {
            page(x,ss,"Địa điểm","places",visitor.places(api.pois(ss,false),q.getOrDefault("q","")));
            return;
        } if(p.equals("/place")&&method.equals("GET")) {
            boolean preview="1".equals(q.get("preview"));
            long id=ClientBUS.positive(q.get("id"));
            var poi=ClientBUS.byId(api.pois(ss,preview),id);
            var languages=api.languages(ss);
            String lang=q.getOrDefault("lang",languages.isEmpty()?"vi":Data.text(languages.getFirst(),"languageCode"));
            Map<String,Object> n=api.contents(ss,preview).stream().filter(m->Data.id(m,"poiId")==id&&Data.text(m,"languageCode").equals(lang)).findFirst().orElse(null);
            Map<String,Object> audio=null;
            String error="";
            try {
                audio=track(ss,n,preview);
                if(audio!=null) {
                    Http.Reply head=api.media(ss,Data.id(audio,"id"),preview,"HEAD",null);
                    if(head.status()!=200) {
                        audio=null;
                        error="Không tải được file MP3 (HTTP "+head.status()+"). Kiểm tra file trên audio-server hoặc tải lại trong quản trị.";
                    }
                }
            }
            catch(Http.Error ex) {
                if(ex.status==401)throw ex;
                error=ex.getMessage();
            }
            page(x,ss,Data.text(poi,"poiName"),"places",visitor.detail(poi,languages,n,audio,lang,preview,error));
            return;
        } if(p.equals("/map")&&method.equals("GET")) {
            page(x,ss,"Bản đồ","map",visitor.map(ss,api.pois(ss,false),id(q),"osm".equals(q.get("basemap"))));
            return;
        } if(p.equals("/simulate")&&method.equals("POST")) {
            double lat=ClientBUS.number(f.get("latitude")),lon=ClientBUS.number(f.get("longitude"));
            var found=ClientBUS.nearest(api.pois(ss,false),lat,lon);
            if(found.isEmpty())success(x,"/map","Chưa có POI nào trong bán kính kích hoạt. Hãy chọn địa điểm thủ công.");
            else success(x,"/place?id="+found.get().get("id"),"Đã xác định POI từ tọa độ mô phỏng.");
            return;
        } if(p.matches("/media/audio/[0-9]+")&&Set.of("GET","HEAD").contains(method)) {
            long id=Long.parseLong(p.substring(p.lastIndexOf('/')+1));
            boolean preview="1".equals(q.get("preview"));
            Http.proxy(x,api.media(ss,id,preview,method,x.getRequestHeaders().getFirst("Range")));
            return;
        } if(p.matches("/media/poi/[0-9]+")&&method.equals("GET")) {
            poiImage(x,ss,Long.parseLong(p.substring(p.lastIndexOf('/')+1)));
            return;
        } if(p.equals("/status")&&method.equals("GET")) {
            page(x,ss,"Trạng thái kết nối","status",admin.status(api.health()));
            return;
        } if(p.equals("/admin")&&method.equals("GET")) {
            page(x,ss,"Quản trị","admin",admin.overview(ss,api.pois(ss,true),api.contents(ss,true),api.languages(ss)));
            return;
        } if(p.equals("/admin/pois")&&method.equals("GET")) {
            page(x,ss,"Quản lý POI","admin",admin.pois(ss,api.pois(ss,true),q.getOrDefault("q","")));
            return;
        } if(p.equals("/admin/poi")&&method.equals("GET")) {
            long id=id(q);
            Map<String,Object> value=id==0?new LinkedHashMap<>():ClientBUS.byId(api.pois(ss,true),id);
            page(x,ss,"Biên tập POI","admin",admin.poiForm(ss,value,api.areas(ss),""));
            return;
        } if(p.equals("/admin/poi/save")&&method.equals("POST")) {
            try {
                api.savePoi(ss,id(f),ClientBUS.poiForm(f));
                success(x,"/admin/pois","Đã lưu POI thành công.");
            }
            catch(Http.Error ex) {
                if(ex.status==401)throw ex;
                page(x,ss,"Biên tập POI","admin",admin.poiForm(ss,new LinkedHashMap<>(f),api.areas(ss),ex.getMessage()));
            }
            return;
        } if(p.equals("/admin/poi/visibility")&&method.equals("GET")) {
            page(x,ss,"Trạng thái POI","admin",admin.confirmVisibility(ss,ClientBUS.byId(api.pois(ss,true),id(q))));
            return;
        } if(p.equals("/admin/poi/visibility")&&method.equals("POST")) {
            api.visible(ss,id(f),"true".equals(f.get("visible")));
            success(x,"/admin/pois","Đã cập nhật trạng thái POI.");
            return;
        } if(p.equals("/admin/contents")&&method.equals("GET")) {
            page(x,ss,"Nội dung thuyết minh","admin",admin.contents(ss,api.contents(ss,true),api.pois(ss,true),q.getOrDefault("q","")));
            return;
        } if(p.equals("/admin/content")&&method.equals("GET")) {
            long id=id(q);
            var content=id==0?new LinkedHashMap<String,Object>():ClientBUS.byId(api.contents(ss,true),id);
            page(x,ss,"Biên tập thuyết minh","admin",admin.contentForm(ss,content,api.pois(ss,true),api.languages(ss),id==0?null:track(ss,content,true),""));
            return;
        } if(p.equals("/admin/content/save")&&method.equals("POST")) {
            try {
                long id=api.saveContent(ss,id(f),ClientBUS.contentForm(f));
                success(x,"/admin/content?id="+id,"Đã lưu nội dung. Bạn có thể tải MP3 tương ứng.");
            }
            catch(Http.Error ex) {
                if(ex.status==401)throw ex;
                Map<String,Object> value=new LinkedHashMap<>(f);
                value.put("visible","true".equals(f.get("visible")));
                page(x,ss,"Biên tập thuyết minh","admin",admin.contentForm(ss,value,api.pois(ss,true),api.languages(ss),null,ex.getMessage()));
            }
            return;
        } if(p.equals("/admin/audio/upload")&&method.equals("POST")) {
            long id=id(f);
            var content=ClientBUS.byId(api.contents(ss,true),id);
            try {
                Multipart.Part file=parts.get("file");
                if(file==null||file.filename().isBlank())throw new Http.Error(400,"Vui lòng chọn MP3.");
                api.upload(ss,id,file.filename(),file.bytes());
                success(x,"/admin/content?id="+id,"Đã gắn MP3 vào nội dung. Nghe thử ngay bên dưới.");
            }
            catch(Http.Error ex) {
                if(ex.status==401)throw ex;
                page(x,ss,"Tải audio","admin",admin.contentForm(ss,content,api.pois(ss,true),api.languages(ss),track(ss,content,true),ex.getMessage()));
            }
            return;
        } throw new Http.Error(404,"Trang không tồn tại.");
    } private void asset(HttpExchange x,String path)throws Exception {
        if(!path.matches("/assets/[a-z0-9.-]+\\.(css|svg|png)"))throw new Http.Error(404,"Asset not found");
        try(var in=ClientController.class.getResourceAsStream(path)) {
            if(in==null)throw new Http.Error(404,"Asset not found");
            Http.bytes(x,200,path.endsWith("css")?"text/css; charset=utf-8":path.endsWith("png")?"image/png":"image/svg+xml",in.readAllBytes());
        }
    } private void poiImage(HttpExchange x,ClientSessionDTO ss,long id)throws Exception {
        try {
            var p=ClientBUS.byId(api.pois(ss,ss.loggedIn()),id);
            String u=Data.text(p,"imageUrl");
            URI base=URI.create(s.get("poi.url","http://127.0.0.1:8082")),uri=base.resolve(u);
            boolean samePort=uri.getPort()==base.getPort();
            boolean sameHost=Objects.equals(uri.getHost(),base.getHost())||Set.of("localhost","127.0.0.1").contains(uri.getHost())&&Set.of("localhost","127.0.0.1").contains(base.getHost());
            if(!sameHost||!samePort||!Set.of("http","https").contains(uri.getScheme()))throw new IllegalArgumentException();
            Http.Reply r=Http.request("GET",uri.toString(),"",null,null,Map.of());
            if(r.status()!=200||!r.header("Content-Type").startsWith("image/")||r.body().length>8*1024*1024)throw new IllegalArgumentException();
            Http.bytes(x,200,r.header("Content-Type"),r.body());
        }
        catch(Exception e) {
            asset(x,"/assets/campus-"+((id-1)%3)+".svg");
        }
    }
}
