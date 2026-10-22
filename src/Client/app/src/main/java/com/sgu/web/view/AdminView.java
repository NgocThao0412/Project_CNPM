package com.sgu.web.view;
import com.sgu.core.*;
import com.sgu.web.DTO.*;
import java.util.*;
import static com.sgu.web.view.Html.*;
public final class AdminView {
    private final Settings s;
    private final Layout layout;
    public AdminView(Settings s) {
        this.s=s;
        this.layout=new Layout(s);
    }
    
public String login(ClientSessionDTO ss, String error, String username) {
    return "<div class=\"login-grid\">"
        + "<section class=\"login-intro\">"
        + "<span class=\"eyebrow\">SAIGON AUDIO GUIDE · ADMIN</span>"
        + "<h1>Chăm chút từng<br>câu chuyện.<br>Kết nối từng<br>hành trình.</h1>"
        + "<p>Không gian dành cho người quản lý địa điểm và nội dung thuyết minh.</p>"
        + "<img src=\"/assets/campus-1.svg\" alt=\"Minh họa khuôn viên\">"
        + "</section>"

        + "<section class=\"login-form\">"
        + "<span class=\"eyebrow\">WELCOME BACK</span>"
        + "<h2>Đăng nhập quản trị</h2>"
        + "<p>Sử dụng tài khoản được cấp bởi hệ thống.</p>"

        + (error == null || error.isBlank() ? "" : layout.error(error))

        + "<form method=\"post\" action=\"/login\">"
        + hidden("csrf", ss.csrf)

        + input(
            "Tên đăng nhập",
            "username",
            username,
            "text",
            "required maxlength=\"50\" autocomplete=\"username\" placeholder=\"Nhập username\""
        )

        + input(
            "Mật khẩu",
            "password",
            "",
            "password",
            "required autocomplete=\"current-password\" placeholder=\"Nhập mật khẩu\""
        )

        + "<button class=\"btn\">"
        + icon("lock")
        + "Đăng nhập"
        + "</button>"
        + "</form>"

        + link("/", "← Quay lại trang khách", "btn btn-ghost")
        + "</section>"
        + "</div>";
}

private static String dashboardMetric(String style, String label, long number,
                                      String ico, String description) {
    return "<div class=\"dash-stat " + style + "\">"
            + "<div class=\"dash-stat-heading\">" + icon(ico)
            + "<span>" + e(description) + "</span></div>"
            + "<strong>" + number + "</strong><span class=\"dash-stat-label\">"
            + e(label) + "</span></div>";
}

public String overview(ClientSessionDTO ss, List<Map<String,Object>> pois,
                       List<Map<String,Object>> contents, List<Map<String,Object>> langs) {
    String person = Layout.adminName(ss);
    java.time.ZonedDateTime time = java.time.ZonedDateTime.now(
            java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    String date = time.format(java.time.format.DateTimeFormatter.ofPattern(
            "EEEE, dd/MM/yyyy", java.util.Locale.forLanguageTag("vi-VN")));
    long shown = pois.stream().filter(p -> Data.bool(p, "visible", true)).count();

    StringBuilder b = new StringBuilder();
    b.append("<section class=\"admin-welcome\"><div class=\"admin-welcome-copy\">"
            + "<span class=\"eyebrow\">SAIGON AUDIO GUIDE · QUẢN TRỊ</span>"
            + "<h1>Xin chào, <span>")
            .append(e(person)).append("!</span></h1>"
            + "<p>Quản lý hành trình khám phá, điểm tham quan và các bài thuyết minh về "
            + "Trường Đại học Sài Gòn.</p>"
            + "<span class=\"admin-welcome-date\">").append(icon("clock"))
            .append(e(date)).append("</span></div></section>");

    b.append("<section class=\"dash-stats\" aria-label=\"Tổng quan dữ liệu\">")
     .append(dashboardMetric("stat-blue", "Địa điểm", pois.size(), "pin", "Tổng số POI"))
     .append(dashboardMetric("stat-green", "Đang hiển thị", shown, "map", "Khách có thể xem"))
     .append(dashboardMetric("stat-violet", "Bài thuyết minh", contents.size(), "book", "Nội dung đã tạo"))
     .append(dashboardMetric("stat-orange", "Ngôn ngữ", langs.size(), "globe", "Đang hỗ trợ"))
     .append("</section>");

    // Online counters are populated by a 5-second Client poll. Never invent LIVE values.
    b.append("<section class=\"visitor-live-strip\" aria-label=\"Người dùng trực tuyến\">"
        + "<div class=\"visitor-live-heading\"><h2>Khách tham quan trực tuyến</h2>"
        + "<p data-visitor-note>Đang kiểm tra dịch vụ người dùng…</p></div>"
        + "<div class=\"visitor-live-cards\">"
        + "<div class=\"visitor-live-card\"><span>Đang truy cập</span><strong data-online-count>—</strong></div>"
        + "<div class=\"visitor-live-card visitor-listening\"><span>Đang nghe thuyết minh</span>"
        + "<strong data-listening-count>—</strong></div></div>"
        + link("/admin/users", "Xem người dùng →", "visitor-live-link") + "</section>");

    b.append("<section class=\"admin-action-panel\"><h2>")
     .append(icon("plus")).append("Thao tác nhanh</h2><div class=\"admin-actions\">")
     .append(link("/admin/poi", icon("pin") + "Thêm địa điểm", "dash-action action-blue"))
     .append(link("/admin/content", icon("book") + "Thêm thuyết minh", "dash-action action-green"))
     .append(link("/admin/contents", icon("audio") + "Quản lý audio", "dash-action action-violet"))
     .append(link("/status", icon("cloud") + "Kiểm tra API", "dash-action action-light"))
     .append("</div></section>");

    // These figures are derived from POI, narration and language APIs.
    // The group has not supplied a listening-analytics API, so we do not
    // draw fictitious 7-day play counts or a fabricated activity feed.
    Map<Long, Integer> countByPoi = new HashMap<>();
    for (var n : contents) {
        long id = Data.id(n, "poiId");
        countByPoi.merge(id, 1, Integer::sum);
    }
    b.append("<div class=\"admin-insights\"><section class=\"dash-panel\">"
            + "<div class=\"dash-panel-title\"><h2>").append(icon("book"))
     .append("Mức độ phủ thuyết minh</h2><span>Theo POI và ngôn ngữ</span></div>");
    if (pois.isEmpty()) {
        b.append("<p class=\"dash-empty\">Chưa có địa điểm. Hãy thêm POI để bắt đầu.</p>");
    } else {
        int denominator = Math.max(langs.size(), 1);
        int shownRows = 0;
        for (var p : pois) {
            if (shownRows++ == 6) break;
            int count = countByPoi.getOrDefault(Data.id(p, "id"), 0);
            int width = Math.max(0, Math.min(100, (int) Math.round(count * 100.0 / denominator)));
            b.append("<div class=\"coverage-row\"><div class=\"coverage-label\"><strong>")
             .append(e(Data.text(p,"poiName"))).append("</strong><span>")
             .append(count).append(" / ").append(langs.size())
             .append(" ngôn ngữ</span></div><div class=\"coverage-track\"><span style=\"width:")
             .append(width).append("%\"></span></div></div>");
        }
    }
    b.append("<div class=\"dash-panel-footer\">")
     .append(link("/admin/pois", "Xem danh sách địa điểm →", "dash-text-link"))
     .append("</div></section>");

    Map<Long,String> names = new HashMap<>();
    for (var p:pois) names.put(Data.id(p,"id"), Data.text(p,"poiName"));
    b.append("<section class=\"dash-panel\"><div class=\"dash-panel-title\"><h2>")
     .append(icon("audio")).append("Thuyết minh đang quản lý</h2>"
            + "<span>Nội dung hiện có</span></div>");
    if (contents.isEmpty()) {
        b.append("<p class=\"dash-empty\">Chưa có bài thuyết minh nào. Bạn có thể tạo bài đầu tiên.</p>");
    } else {
        int rows = 0;
        for (var n : contents) {
            if (rows++ == 5) break;
            b.append("<a class=\"dash-story-row\" href=\"/admin/content?id=")
             .append(Data.id(n,"id")).append("\"><span class=\"dash-story-icon\">")
             .append(icon("book")).append("</span><span class=\"dash-story-text\"><strong>")
             .append(e(Data.text(n,"title"))).append("</strong><small>")
             .append(e(names.getOrDefault(Data.id(n,"poiId"), "POI #" + n.get("poiId"))))
             .append(" · ").append(e(Data.text(n,"languageName")))
             .append("</small></span><span class=\"dash-story-chevron\">›</span></a>");
        }
    }
    b.append("<div class=\"dash-panel-footer\">")
     .append(link("/admin/contents", "Quản lý tất cả bài thuyết minh →", "dash-text-link"))
     .append("</div></section></div>");

    return b.toString();
}

    /** Separate route: audio and contents must never share the same highlighted menu item. */
    public String audios(ClientSessionDTO ss,List<Map<String,Object>> contents,
                         List<Map<String,Object>> pois) {
        Map<Long,String> names = new HashMap<>();
        for(var p:pois) names.put(Data.id(p,"id"),Data.text(p,"poiName"));
        StringBuilder b=new StringBuilder(layout.adminNav(ss,"contents")
            + "<div class=\"section-header\"><div><h2>Quản lý audio</h2>"
            + "<p>Chọn bài thuyết minh để tải MP3, thay bản audio hoặc nghe thử.</p></div>"
            + link("/admin/contents","Quản lý nội dung","btn btn-light") + "</div>"
            + "<section class=\"panel\"><div class=\"table-wrap\"><table class=\"data-table\">"
            + "<thead><tr><th>Bài thuyết minh</th><th>Địa điểm</th><th>Ngôn ngữ</th><th>Thao tác</th>"
            + "</tr></thead><tbody>");
        for(var n:contents) {
            long id=Data.id(n,"id"), poiId=Data.id(n,"poiId");
            b.append("<tr><td><strong>").append(e(n.get("title"))).append("</strong></td>"
               + "<td>").append(e(names.getOrDefault(poiId,"POI #"+poiId))).append("</td>"
               + "<td>").append(e(n.get("languageName"))).append("</td><td>")
             .append(link("/admin/content?id="+id+"&from=audio",icon("audio")+" Tải / thay MP3",
                     "btn btn-light btn-small"))
             .append("</td></tr>");
        }
        if(contents.isEmpty())b.append("<tr><td colspan=\"4\">Chưa có bài thuyết minh. "
             + "Hãy tạo nội dung trước khi tải MP3.</td></tr>");
        return b.append("</tbody></table></div></section>").toString();
    }

    /** Backend reports aggregate visitor and listener counts, not named accounts. */
    public String users() {
        return "<div class=\"section-header\"><div><h2>Người dùng · Khách tham quan</h2>"
            + "<p>Thống kê các phiên truy cập ẩn danh và người đang nghe thuyết minh.</p></div></div>"
            + "<section class=\"visitor-users-summary\">"
            + "<div class=\"visitor-users-tile\"><span>Đang truy cập</span><strong data-online-count>—</strong></div>"
            + "<div class=\"visitor-users-tile listening\"><span>Đang nghe</span>"
            + "<strong data-listening-count>—</strong></div></section>"
            + "<section class=\"panel\"><div class=\"visitor-users-title\">"
            + "<h3>Trạng thái kết nối visitor-service</h3>"
            + "<p data-visitor-note>Đang lấy thống kê từ API…</p></div>"
            + "<p class=\"help\">Đang nghe là tập con của đang truy cập, không cộng hai số lại. "
            + "Khách không cần đăng nhập. Client gửi heartbeat mỗi 10 giây; "
            + "Backend chỉ tính phiên nhận tín hiệu trong 30 giây gần nhất. "
            + "Các số tự cập nhật sau mỗi 5 giây.</p></section>";
    }

    public String pois(ClientSessionDTO ss,List<Map<String,Object>> pois,String q) {
        StringBuilder table=new StringBuilder("<div class=\"table-wrap\"><table class=\"data-table\"><thead><tr><th>Địa điểm</th><th>Khu vực</th><th>GPS</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>");
        String term=q.toLowerCase(Locale.ROOT);
        for(var p:pois) {
            if(!(Data.text(p,"poiName")+Data.text(p,"poiCode")).toLowerCase(Locale.ROOT).contains(term))continue;
            long id=Data.id(p,"id");
            boolean visible=Data.bool(p,"visible",true);
            table.append("<tr><td><img class=\"table-avatar\" src=\"").append(VisitorView.picture(p)).append("\" alt=\"\"><strong>").append(e(p.get("poiName"))).append("</strong><small>").append(e(p.get("poiCode"))).append("</small></td><td data-label=\"Khu vực\">").append(e(p.get("areaName"))).append("</td><td data-label=\"GPS\"><span>").append(e(p.get("latitude"))).append(", ").append(e(p.get("longitude"))).append("</span><small>Bán kính ").append(e(p.get("triggerRadiusM"))).append(" m</small></td><td><span class=\"tag ").append(visible?"tag-green":"tag-muted").append("\">").append(visible?"Đang hiển thị":"Đã ẩn").append("</span></td><td><div class=\"row-actions\">").append(link("/admin/poi?id="+id,icon("edit")+"Sửa","btn btn-light btn-small")).append(link("/admin/poi/visibility?id="+id,visible?"Ẩn":"Hiện","btn btn-light btn-small")).append("</div></td></tr>");
        }
        table.append("</tbody></table></div>");
        return layout.adminNav(ss,"pois")+"<div class=\"section-header\"><div><h2>Danh sách POI</h2><p>"+pois.size()+" địa điểm · Bao gồm cả POI đã ẩn</p></div>"+link("/admin/poi",icon("plus")+"Thêm POI","btn")+"</div>"+VisitorView.search("/admin/pois",q,"Tìm theo tên hoặc mã POI...")+(pois.isEmpty()?"<div class=\"empty\">Chưa có POI. Hãy thêm địa điểm đầu tiên.</div>":table);
    }
    public String poiForm(ClientSessionDTO ss,Map<String,Object> value,List<Map<String,Object>> areas,String error) {
        long id=value.containsKey("id")?Data.id(value,"id"):0;
        return layout.adminNav(ss,"pois")+"<div class=\"narrow\"><section class=\"panel\"><span class=\"eyebrow\">PLACE EDITOR</span><h2>"+(id==0?"Thêm địa điểm":"Chỉnh sửa địa điểm")+"</h2><p class=\"help\">Tọa độ dùng để hiển thị bản đồ và xác định POI trong bán kính kích hoạt.</p>"+(error.isBlank()?"":layout.error(error))+"<form method=\"post\" action=\"/admin/poi/save\">"+hidden("csrf",ss.csrf)+hidden("id",id)+"<div class=\"form-grid\">"+input("Mã POI","poiCode",value.get("poiCode"),"text","required maxlength=\"50\" placeholder=\"SGU-004\"")+input("Tên địa điểm","poiName",value.get("poiName"),"text","required maxlength=\"200\"")+"<div class=\"full\">"+select("Khu vực","areaId",areas,"areaName",value.getOrDefault("areaId",areas.isEmpty()?0:areas.getFirst().get("id")),false)+"</div><div class=\"full\">"+area("Mô tả địa điểm","description",value.get("description"),4)+"</div>"+input("Vĩ độ","latitude",value.get("latitude"),"number","required min=\"-90\" max=\"90\" step=\"0.0000001\"")+input("Kinh độ","longitude",value.get("longitude"),"number","required min=\"-180\" max=\"180\" step=\"0.0000001\"")+input("Bán kính kích hoạt (m)","triggerRadiusM",value.get("triggerRadiusM"),"number","required min=\"0.01\" max=\"999999.99\" step=\"0.01\"")+"</div><div class=\"form-actions\"><button class=\"btn\">"+icon("check")+"Lưu POI</button>"+link("/admin/pois","Hủy","btn btn-light")+"</div></form></section></div>";
    }
    public String confirmVisibility(ClientSessionDTO ss,Map<String,Object> poi) {
        boolean visible=Data.bool(poi,"visible",true);
        return layout.adminNav(ss,"pois")+"<section class=\"panel narrow\"><h2>"+(visible?"Ẩn địa điểm?":"Hiển thị lại địa điểm?")+"</h2><h3>"+e(poi.get("poiName"))+"</h3><p>"+(visible?"POI sẽ không còn hiển thị cho khách. Dữ liệu và lịch sử không bị xóa.":"POI sẽ xuất hiện trở lại khi khu vực cũng đang được hiển thị.")+"</p><form method=\"post\" action=\"/admin/poi/visibility\">"+hidden("csrf",ss.csrf)+hidden("id",poi.get("id"))+hidden("visible",!visible)+"<div class=\"form-actions\"><button class=\"btn "+(visible?"btn-danger":"")+"\">"+(visible?"Xác nhận ẩn":"Xác nhận hiển thị")+"</button>"+link("/admin/pois","Hủy","btn btn-light")+"</div></form></section>";
    }
    public String contents(ClientSessionDTO ss,List<Map<String,Object>> contents,List<Map<String,Object>> pois,String q) {
        Map<Long,String> names=new HashMap<>();
        pois.forEach(m->names.put(Data.id(m,"id"),Data.text(m,"poiName")));
        StringBuilder b=new StringBuilder("<div class=\"table-wrap\"><table class=\"data-table\"><thead><tr><th>Bài thuyết minh</th><th>Địa điểm</th><th>Ngôn ngữ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>");
        for(var n:contents) {
            String poi=names.getOrDefault(Data.id(n,"poiId"),"POI #"+n.get("poiId"));
            if(!(Data.text(n,"title")+poi).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))continue;
            b.append("<tr><td><strong>").append(e(n.get("title"))).append("</strong><small>").append(excerpt(n.get("content"),75)).append("</small></td><td data-label=\"Địa điểm\">").append(e(poi)).append("</td><td><span class=\"tag\">").append(icon("globe")).append(e(n.get("languageName"))).append("</span></td><td><span class=\"tag ").append(Data.bool(n,"visible",true)?"tag-green":"tag-muted").append("\">").append(Data.bool(n,"visible",true)?"Đang hiển thị":"Đã ẩn").append("</span></td><td><div class=\"row-actions\">").append(link("/admin/content?id="+n.get("id"),icon("edit")+"Sửa & MP3","btn btn-light btn-small")).append(link("/place?id="+n.get("poiId")+"&lang="+Http.enc(Data.text(n,"languageCode"))+"&preview=1",icon("audio")+"Nghe thử","btn btn-light btn-small")).append("</div></td></tr>");
        }
        b.append("</tbody></table></div>");
        return layout.adminNav(ss,"contents")+"<div class=\"section-header\"><div><h2>Nội dung thuyết minh</h2><p>Mỗi cặp POI và ngôn ngữ có một bài thuyết minh.</p></div>"+link("/admin/content",icon("plus")+"Thêm nội dung","btn")+"</div>"+VisitorView.search("/admin/contents",q,"Tìm bài thuyết minh hoặc địa điểm...")+b;
    }
    public String contentForm(ClientSessionDTO ss,Map<String,Object> v,List<Map<String,Object>> pois,List<Map<String,Object>> langs,Map<String,Object> audio,String error) {
        long id=v.containsKey("id")?Data.id(v,"id"):0;
        String form="<section class=\"panel\"><span class=\"eyebrow\">STORY EDITOR</span><h2>"+(id==0?"Thêm bài thuyết minh":"Biên tập nội dung")+"</h2>"+(error.isBlank()?"":layout.error(error))+"<form method=\"post\" action=\"/admin/content/save\">"+hidden("csrf",ss.csrf)+hidden("id",id)+"<div class=\"form-grid\">"+select("Địa điểm (POI)","poiId",pois,"poiName",v.getOrDefault("poiId",pois.isEmpty()?0:pois.getFirst().get("id")),id>0)+select("Ngôn ngữ","languageId",langs,"languageName",v.getOrDefault("languageId",langs.isEmpty()?0:langs.getFirst().get("id")),id>0)+"<div class=\"full\">"+input("Tiêu đề","title",v.get("title"),"text","required maxlength=\"200\"")+"</div><div class=\"full\">"+area("Văn bản thuyết minh","content",v.get("content"),10)+"</div></div><label class=\"check-line\"><input name=\"visible\" value=\"true\" type=\"checkbox\" "+(Data.bool(v,"visible",true)?"checked":"")+">Hiển thị cho khách tham quan</label><p class=\"help\">"+(id>0?"Không đổi POI/ngôn ngữ của bài cũ để giữ đúng lịch sử audio. Cần đổi thì tạo bài mới.":"Lưu nội dung trước, sau đó tải MP3 tương ứng.")+"</p><div class=\"form-actions\"><button class=\"btn\">"+icon("check")+"Lưu nội dung</button>"+link("/admin/contents","Hủy","btn btn-light")+"</div></form></section>";
        String upload="<aside class=\"panel\"><div class=\"player-heading\">"+icon("audio")+"<div><h2>Audio thuyết minh</h2><p>MP3 · tối đa 25 MB</p></div></div>";
        if(id==0)upload+="<div class=\"tip\">"+icon("book")+"<span>Lưu bài thuyết minh để có mã nội dung, rồi gắn audio vào đúng bài.</span></div>";
        else {
            if(audio!=null)upload+="<strong>"+e(audio.get("fileName"))+"</strong><p class=\"help\">"+seconds(audio.get("durationSeconds"))+" · Phiên bản "+e(audio.get("versionNumber"))+"</p><audio controls preload=\"metadata\" src=\"/media/audio/"+Data.id(audio,"id")+"?preview=1\"></audio>";
            else upload+="<p>Chưa gắn MP3 cho nội dung này.</p>";
            upload+="<form method=\"post\" enctype=\"multipart/form-data\" action=\"/admin/audio/upload\">"+hidden("csrf",ss.csrf)+hidden("id",id)+"<label class=\"field\"><span>Chọn MP3 từ máy</span><input type=\"file\" name=\"file\" accept=\"audio/mpeg,.mp3\" required></label><p class=\"help\">Tải file mới sẽ tạo phiên bản mới. Bản cũ được giữ lại, không ghi đè.</p><button class=\"btn\">"+icon("cloud")+(audio==null?"Tải MP3 lên":"Thay audio")+"</button></form><hr class=\"player-separator\">"+link("/place?id="+v.get("poiId")+"&lang="+Http.enc(Data.text(v,"languageCode"))+"&preview=1",icon("audio")+"Nghe thử trang chi tiết","btn btn-light");
        } return layout.adminNav(ss,"contents")+"<div class=\"upload-grid\">"+form+upload+"</aside></div>";
    }
    public String status(List<Map<String,Object>> health) {
        StringBuilder b=new StringBuilder("<div class=\"page-intro\"><div><span class=\"eyebrow\">CONNECTION CHECK</span><h1>Trạng thái kết nối</h1></div>"+link("/status","Kiểm tra lại","btn btn-light")+"</div><section class=\"panel\"><div class=\"status-list\">");
        for(var r:health)b.append("<div class=\"status-row\"><div><strong>").append(e(r.get("service"))).append("</strong><small class=\"api-details\">").append(e(r.get("url"))).append("</small></div><span class=\"tag ").append("OK".equals(r.get("status"))?"tag-green":"tag-muted").append("\">").append(e(r.get("status"))).append("</span></div>");
        return b.append("</div><p class=\"help section-space\">UP chỉ xác nhận server đang chạy. Để kiểm tra dữ liệu, hãy đăng nhập, thêm POI, thêm nội dung, tải MP3 và nghe trên trang khách.</p></section>").toString();
    }
}
