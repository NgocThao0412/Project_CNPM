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
        + "<span class=\"eyebrow\">SAIGON UNIVERSITY · ADMIN</span>"
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

private static String metric(String label, long number, String ic) {
    return "<div class=\"metric\">"
            + icon(ic)
            + "<strong>" + number + "</strong>"
            + "<p>" + e(label) + "</p>"
            + "</div>";
}


    public String overview(ClientSessionDTO ss,List<Map<String,Object>> pois,List<Map<String,Object>> contents,List<Map<String,Object>> langs) {
        return layout.adminNav(ss,"overview")+"<div class=\"metrics\">"+metric("Địa điểm",pois.size(),"pin")+metric("POI đang hiển thị",pois.stream().filter(m->Data.bool(m,"visible",true)).count(),"map")+metric("Bài thuyết minh",contents.size(),"book")+metric("Ngôn ngữ",langs.size(),"globe")+"</div><div class=\"upload-grid\"><section class=\"panel\"><span class=\"eyebrow\">01 · PLACES</span><h2>Quản lý điểm tham quan</h2><p>Thêm địa điểm, sửa thông tin, cấu hình GPS và kiểm soát trạng thái hiển thị.</p>"+link("/admin/pois","Mở danh sách POI "+icon("arrow"),"btn")+"</section><section class=\"panel\"><span class=\"eyebrow\">02 · STORIES</span><h2>Nội dung & audio</h2><p>Chọn POI, ngôn ngữ, biên tập nội dung và tải bản MP3 tương ứng.</p>"+link("/admin/contents","Mở nội dung "+icon("arrow"),"btn btn-light")+"</section></div><div class=\"guide-strip\"><div class=\"icon-tile\">"+icon("cloud")+"</div><div class=\"copy\"><h3>Kiểm tra kết nối dữ liệu</h3><p>Mỗi thao tác được gửi qua gateway tới server phụ trách. Không lưu giả thành công khi API báo lỗi.</p></div>"+link("/status","Trạng thái server","btn btn-light")+"</div>";
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
