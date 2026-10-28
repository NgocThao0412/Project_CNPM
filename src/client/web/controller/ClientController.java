package web.controller;
import core.*;
import web.BUS.*;
import web.DAO.*;
import web.DTO.*;
import web.view.*;
import gateway.controller.GatewayController;
import com.sun.net.httpserver.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
/** Java Client web UI with minimal browser-side activity and audio event hooks. */
public final class ClientController {
    private final Settings s;
    private final ApiDAO api;
    private final VisitorActivityDAO activity;
    private final GatewayController gateway;
    private final Layout layout;
    private final VisitorView visitor;
    private final AdminView admin;
    private final Map<String,ClientSessionDTO> sessions=new ConcurrentHashMap<>();
    public ClientController(Settings s) throws Exception {
        this.s=s;
        gateway=new GatewayController(s);
        api=new ApiDAO(s);
        activity=new VisitorActivityDAO(s);
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
    } private void setLanguageCookie(HttpExchange x,String lang) {
        String secure=Boolean.parseBoolean(s.get("web.secure.cookie","false"))?"; Secure":"";
        x.getResponseHeaders().add("Set-Cookie","SGU_LANG="+UiLanguage.normalize(lang)
                +"; Path=/; SameSite=Lax; Max-Age=31536000; HttpOnly"+secure);
    } private void csrf(ClientSessionDTO ss,String token) {
        if(token==null||!MessageDigest.isEqual(ss.csrf.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))throw new Http.Error(403,"Biểu mẫu đã hết hạn. Tải lại trang rồi thử lại.");
    } private void page(HttpExchange x,ClientSessionDTO ss,String title,String tab,String body)throws Exception {
        String note=Http.query(x).getOrDefault("notice","");
        if(note.length()>300)note=note.substring(0,300);
        // Keep per-session navigation history inside the admin area only.
        if ("GET".equals(x.getRequestMethod())
                && ("admin".equals(tab) || ("status".equals(tab) && ss.loggedIn()))) {
            String current = x.getRequestURI().getRawPath();
            String query = x.getRequestURI().getRawQuery();
            if (query != null && !query.isEmpty()) current += "?" + query;
            ss.rememberAdminPage(current);
        }
        Http.html(x,layout.page(ss,title,tab,body,note,UiLanguage.selected(x)));
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
        // These four endpoints are the team's visitor-service contract. All browser
        // requests stay on Client port 8080; Java forwards to 8085 in LIVE mode.
        if(p.equals("/api/visitor/sessions")
        || p.equals("/api/visitor/heartbeat")
        || p.equals("/api/visitor/end")
        || p.equals("/api/visitor/listening-events")) {
            visitorApi(x,p,method);
            return;
        }
        if(p.equals("/api/admin/visitor-stats")) {
            if(!method.equals("GET"))throw new Http.Error(405,"Method not allowed");
            ClientSessionDTO adminSession=session(x);
            api.me(adminSession);
            Http.json(x,200,activity.stats(adminSession));
            return;
        }
        if (p.equals("/api/admin/listening-stats")) {
            x.getResponseHeaders().set("Cache-Control", "no-store");

            if (!method.equals("GET")) {
                x.getResponseHeaders().set("Allow", "GET");
                throw new Http.Error(405, "Method not allowed");
            }

            ClientSessionDTO adminSession = session(x);

            // Kiểm tra đăng nhập với auth-service.
            api.me(adminSession);

            Http.json(
                    x,
                    200,
                    activity.listeningStats(
                            adminSession,
                            Http.query(x)
                    )
            );
            return;
        }
        // Existing service routes continue through the built-in gateway.
        if(p.startsWith("/api/")) {
            gateway.handle(x);
            return;
        }
        if(p.startsWith("/assets/")) { asset(x,p); return; }
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
            Http.html(x,e.status,layout.page(ss,"Thông báo","","<section class=\"panel error-page\"><span class=\"eyebrow\">SAIGON AUDIO GUIDE</span><h1>Chưa thể tải nội dung</h1>"+layout.error(e.getMessage())+Html.link("/status","Kiểm tra kết nối","btn")+" "+Html.link("/","Về trang chủ","btn btn-light")+"</section>",""));
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
        if(p.equals("/language")) {
            if(!method.equals("GET")) throw new Http.Error(405,"Method not allowed");
            String chosen=q.getOrDefault("lang","");
            if(!UiLanguage.supported(chosen)) throw new Http.Error(400,"Ngôn ngữ không hợp lệ.");
            String locale=UiLanguage.normalize(chosen);
            setLanguageCookie(x,locale);
            Http.redirect(x,UiLanguage.returnPath(q.get("next"),locale));
            return;
        }
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
            // Use the header language (cookie) or an explicit ?lang= selection.
            String preferred=UiLanguage.selected(x);
            String lang=languages.stream().map(l->Data.text(l,"languageCode"))
                    .filter(code->UiLanguage.normalize(code).equals(preferred))
                    .findFirst().orElse(preferred);
            if(q.containsKey("lang") && UiLanguage.supported(q.get("lang")))
                setLanguageCookie(x,preferred);
            Map<String,Object> n=api.contents(ss,preview).stream()
                    .filter(m->Data.id(m,"poiId")==id
                            && UiLanguage.normalize(Data.text(m,"languageCode")).equals(preferred))
                    .findFirst().orElse(null);
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
        } if(p.equals("/admin/back")&&method.equals("GET")) {
            // Server-side history: the target is never a visitor/external page.
            Http.redirect(x,ss.goBackInAdmin());
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
        } if(p.equals("/admin/users")&&method.equals("GET")) {
            page(x,ss,"Người dùng","admin",admin.users(api.pois(ss, true), api.languages(ss)));
            return;
        } if(p.equals("/admin/audio")&&method.equals("GET")) {
            page(x,ss,"Quản lý audio","admin",
                admin.audios(ss,api.contents(ss,true),api.pois(ss,true)));
            return;
        } if(p.equals("/admin/contents")&&method.equals("GET")) {
            page(x,ss,"Nội dung thuyết minh","admin",admin.contents(ss,api.contents(ss,true),api.pois(ss,true),q.getOrDefault("q","")));
            return;
        } if(p.equals("/admin/content")&&method.equals("GET")) {
            long id=id(q);
            var content=id==0?new LinkedHashMap<String,Object>():ClientBUS.byId(api.contents(ss,true),id);
            page(x,ss,"audio".equals(q.get("from"))?"Quản lý audio":"Biên tập thuyết minh",
                "admin",admin.contentForm(ss,content,api.pois(ss,true),api.languages(ss),
                id==0?null:track(ss,content,true),""));
            return;
        } if(p.equals("/admin/content/translate")&&method.equals("POST")) {
            var pois = api.pois(ss,true);
            var languages = api.languages(ss);

            try {
                Map<String,Object> original = ClientBUS.contentForm(f);

                long sourceLanguageId = Data.id(original,"languageId");
                long targetLanguageId = ClientBUS.positive(
                        f.get("targetLanguageId")
                );
                long poiId = Data.id(original,"poiId");

                ClientBUS.byId(pois,poiId);

                var sourceLanguage = ClientBUS.byId(
                        languages,sourceLanguageId
                );
                var targetLanguage = ClientBUS.byId(
                        languages,targetLanguageId
                );

                String sourceCode = Data.text(
                        sourceLanguage,"languageCode"
                );
                String targetCode = Data.text(
                        targetLanguage,"languageCode"
                );

                if(sourceLanguageId == targetLanguageId) {
                    throw new Http.Error(
                            400,"Hãy chọn ngôn ngữ đích khác ngôn ngữ gốc."
                    );
                }

                for(var existing : api.contents(ss,true)) {
                    if(Data.id(existing,"poiId") == poiId
                            && Data.id(existing,"languageId") == targetLanguageId) {
                        throw new Http.Error(
                                409,
                                "POI đã có bài bằng ngôn ngữ đích. "
                                + "Hãy mở bài đó để chỉnh sửa."
                        );
                    }
                }

                var translated = api.translateContent(
                        ss,
                        sourceCode,
                        targetCode,
                        Data.text(original,"title"),
                        Data.text(original,"content")
                );

                Map<String,Object> draft = new LinkedHashMap<>();
                draft.put("id",0);
                draft.put("poiId",poiId);
                draft.put("languageId",targetLanguageId);
                draft.put("languageCode",targetCode);
                draft.put("title",Data.text(translated,"title"));
                draft.put("content",Data.text(translated,"content"));
                draft.put("visible",false);
                draft.put(
                        "translationNotice",
                        "Đã dịch tự động. Hãy kiểm tra nội dung rồi bấm "
                        + "Lưu nội dung. Bài gốc vẫn được giữ nguyên."
                );

                page(
                        x,ss,"Kiểm tra bản dịch","admin",
                        admin.contentForm(
                                ss,draft,pois,languages,null,""
                        )
                );
            }
            catch(Http.Error ex) {
                if(ex.status == 401)throw ex;

                Map<String,Object> value = new LinkedHashMap<>(f);
                value.put("visible","true".equals(f.get("visible")));

                page(
                        x,ss,"Biên tập thuyết minh","admin",
                        admin.contentForm(
                                ss,value,pois,languages,null,ex.getMessage()
                        )
                );
            }
            return;
        }
        if(p.equals("/admin/content/auto")&&method.equals("POST")) {
            try {
                var value = ClientBUS.contentForm(f);
                var language = ClientBUS.byId(
                        api.languages(ss),
                        Data.id(value,"languageId")
                );

                if(!"vi".equals(Data.text(language,"languageCode"))) {
                    throw new Http.Error(
                            400,
                            "Hãy chọn bài tiếng Việt làm nội dung gốc."
                    );
                }

                var job = api.startAutoNarration(
                        ss,
                        Data.row(
                                "poiId", Data.id(value,"poiId"),
                                "title", Data.text(value,"title"),
                                "content", Data.text(value,"content"),
                                "visible", Data.bool(value,"visible",false),
                                "replaceExisting",
                                "true".equals(f.get("replaceExisting"))
                        )
                );

                String jobId = Data.text(job,"jobId");

                if(jobId.isBlank()) {
                    throw new Http.Error(
                            502,"Server chưa trả mã tác vụ."
                    );
                }

                Http.redirect(
                        x,
                        "/admin/content/auto?jobId=" + Http.enc(jobId)
                );
            }
            catch(Http.Error ex) {
                if(ex.status == 401)throw ex;

                Map<String,Object> value = new LinkedHashMap<>(f);
                value.put("visible","true".equals(f.get("visible")));

                page(
                        x,ss,"Tạo thuyết minh tự động","admin",
                        admin.contentForm(
                                ss,value,api.pois(ss,true),
                                api.languages(ss),null,ex.getMessage()
                        )
                );
            }
            return;
        }

        if(p.equals("/admin/content/auto/status")&&method.equals("GET")) {
            var status = api.autoNarrationProgress(
                    ss,q.getOrDefault("jobId","")
            );

            Http.json(x,200,status);
            return;
        }

        if(p.equals("/admin/content/auto")&&method.equals("GET")) {
            String jobId = q.getOrDefault("jobId","");

            // Kiểm tra tác vụ tồn tại và phiên quản trị còn hợp lệ.
            api.autoNarrationProgress(ss,jobId);

            page(
                    x,ss,"Tiến độ tạo thuyết minh","admin",
                    admin.autoProgress(ss,jobId)
            );
            return;
        }

        if(p.equals("/admin/content/save")&&method.equals("POST")) {
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
    } private void visitorApi(HttpExchange x,String path,String method)throws Exception {
        if(!method.equals("POST"))throw new Http.Error(405,"Method not allowed");
        // Same-origin browser POSTs only. Never proxy arbitrary visitor headers.
        String site=x.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if(site!=null && !site.equals("same-origin") && !site.equals("none"))
            throw new Http.Error(403,"Chỉ chấp nhận yêu cầu cùng nguồn.");
        String origin=x.getRequestHeaders().getFirst("Origin");
        String host=x.getRequestHeaders().getFirst("Host");
        if(origin!=null && !origin.isBlank()) {
            try {
                URI u=URI.create(origin);
                if(!Set.of("http","https").contains(u.getScheme()) ||
                        host==null || !host.equalsIgnoreCase(u.getAuthority()))
                    throw new Http.Error(403,"Nguồn gửi không hợp lệ.");
            } catch(IllegalArgumentException e) {
                throw new Http.Error(403,"Nguồn gửi không hợp lệ.");
            }
        }
        if(path.equals("/api/visitor/sessions")) {
            // A brand-new anonymous session is issued by visitor-service, not a browser UUID.
            Http.json(x,200,Data.row("token",activity.create()));
            return;
        }
        String token=activity.checkedToken(x.getRequestHeaders().getFirst("X-Visitor-Token"));
        if (path.equals("/api/visitor/listening-events")) {
    String contentType = x.getRequestHeaders()
            .getFirst("Content-Type");

    if (contentType == null
            || !contentType.toLowerCase(Locale.ROOT)
                    .startsWith("application/json")) {
        throw new Http.Error(415, "Yêu cầu phải là JSON.");
    }

    byte[] body = x.getRequestBody().readNBytes(2049);

    if (body.length > 2048) {
        throw new Http.Error(413, "Dữ liệu lượt nghe quá lớn.");
    }

    Map<String, Object> value = Json.object(
            new String(body, StandardCharsets.UTF_8)
    );

    if (!(value.get("eventId") instanceof String eventId)
            || !eventId.matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}"
                    + "-[0-9a-f]{4}-[0-9a-f]{12}"
            )) {
        throw new Http.Error(400, "Mã sự kiện không hợp lệ.");
    }

    if (!(value.get("audioFileId") instanceof Number number)
            || !Double.isFinite(number.doubleValue())
            || number.doubleValue() != number.longValue()
            || number.longValue() <= 0
            || number.doubleValue() > 9_007_199_254_740_991d) {
        throw new Http.Error(400, "ID audio không hợp lệ.");
    }

    activity.recordListening(
            token,
            eventId,
            number.longValue()
    );

    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.sendResponseHeaders(204, -1);
    return;
}
        if(path.equals("/api/visitor/heartbeat")) {
            String contentType=x.getRequestHeaders().getFirst("Content-Type");
            if(contentType==null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json"))
                throw new Http.Error(415,"Yêu cầu phải là JSON.");
            byte[] body=x.getRequestBody().readNBytes(1025);
            if(body.length>1024)throw new Http.Error(413,"Heartbeat quá lớn.");
            Map<String,Object> value=Json.object(new String(body,StandardCharsets.UTF_8));
            if(!(value.get("listening") instanceof Boolean listening))
                throw new Http.Error(400,"Trường listening phải là true hoặc false.");
            activity.heartbeat(token,listening);
        } else {
            activity.end(token);
        }
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.sendResponseHeaders(204,-1);
    } private void asset(HttpExchange x,String path)throws Exception {
        if(!path.matches("/assets/[a-z0-9.-]+\\.(css|svg|png|js)"))throw new Http.Error(404,"Asset not found");
        try(var in=ClientController.class.getResourceAsStream(path)) {
            if(in==null)throw new Http.Error(404,"Asset not found");
            Http.bytes(x,200,path.endsWith("css")?"text/css; charset=utf-8":path.endsWith("js")?
                "application/javascript; charset=utf-8":path.endsWith("png")?"image/png":"image/svg+xml",in.readAllBytes());
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
