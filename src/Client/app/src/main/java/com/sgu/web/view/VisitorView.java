package com.sgu.web.view;
import com.sgu.core.*;
import com.sgu.web.DTO.*;
import java.util.*;
import static com.sgu.web.view.Html.*;
/** Server-rendered Java UI shared by iOS Safari, Android Chrome and desktop browsers. */
public final class VisitorView {
    private final Settings s;
    public VisitorView(Settings s) {
        this.s=s;
    }
    public static String picture(Map<String,Object> m) {
        String u=Data.text(m,"imageUrl");
        if(u.startsWith("https://"))return e(u);
        if(!u.isBlank())return "/media/poi/"+Data.id(m,"id");
        return "/assets/campus-"+((Data.id(m,"id")-1)%3)+".svg";
    }
    public static String imageLabel(Map<String,Object> m) {
        return Data.text(m,"imageUrl").isBlank()?"<span class=\"image-label\">Minh họa địa điểm</span>":"";
    } private String cards(List<Map<String,Object>> rows) {
        if(rows.isEmpty())return "<div class=\"empty\"><h3>Chưa có địa điểm phù hợp</h3><p>Thử từ khóa khác hoặc thêm POI trong trang quản trị.</p></div>";
        StringBuilder b=new StringBuilder();
        for(var p:rows) {
            long id=Data.id(p,"id");
            b.append(link("/place?id="+id,"<div class=\"card-image\"><img loading=\"lazy\" src=\""+picture(p)+"\" alt=\""+e(p.get("poiName"))+"\"><span class=\"tag\">"+icon("pin")+e(p.get("poiCode"))+"</span>"+imageLabel(p)+"</div><div class=\"card-body\"><div class=\"region\">"+icon("pin")+e(p.get("areaName"))+"</div><h3>"+e(p.get("poiName"))+"</h3><p>"+excerpt(p.get("description"),114)+"</p><div class=\"card-bottom\"><span>"+icon("audio")+"Khám phá câu chuyện</span><span class=\"go\">"+icon("arrow")+"</span></div></div>","poi-card"));
        }
        return b.toString();
    }
    public String home(List<Map<String,Object>> pois,List<Map<String,Object>> langs) {
        // The campus is a real background image, not a screenshot with text baked in.
        // Live Java-generated links, buttons, and headings stay accessible on phones.
        String hero = "<section class=\"hero hero--immersive\" aria-labelledby=\"hero-heading\">"
            + "<div class=\"hero-copy\">"
            + "<h1 id=\"hero-heading\">Một khuôn viên.<br><em>Muôn câu chuyện.</em></h1>"
            + "<p>Khám phá Trường Đại học Sài Gòn theo cách của bạn. Chọn địa điểm, "
            + "lắng nghe thuyết minh và bắt đầu một hành trình mới.</p>"
            + "<div class=\"hero-buttons\">"
            + link("/places", "<span class=\"hero-play-symbol\" aria-hidden=\"true\">▶</span>"
                    + "Bắt đầu khám phá " + icon("arrow"), "btn hero-primary")
            + link("/map", icon("map") + "Xem bản đồ", "btn btn-light hero-secondary")
            + "</div></div></section>";
        return hero
            + "<section><div class=\"section-header\"><div><span class=\"eyebrow\">YOUR NEXT STOP</span>"
            + "<h2>Điểm dừng chân tiếp theo</h2><p>Mỗi địa điểm là một câu chuyện đang chờ bạn.</p>"
            + "</div>" + link("/places", "Tất cả địa điểm " + icon("arrow"), "btn btn-ghost")
            + "</div><div class=\"cards\">" + cards(pois.stream().limit(6).toList()) + "</div></section>"
            + "<div class=\"guide-strip\"><div class=\"icon-tile\">" + icon("map")
            + "</div><div class=\"copy\"><h3>Chưa đến trường? Vẫn có thể khám phá.</h3>"
            + "<p>Chọn một POI trên bản đồ để mô phỏng vị trí và nghe thuyết minh.</p>"
            + "</div>" + link("/map", "Trải nghiệm mô phỏng " + icon("arrow"), "btn btn-light")
            + "</div>";
    }
    public String places(List<Map<String,Object>> pois,String q) {
        String term=q.toLowerCase(Locale.ROOT).trim();
        var shown=pois.stream().filter(m->(Data.text(m,"poiName")+" "+Data.text(m,"poiCode")+" "+Data.text(m,"areaName")).toLowerCase(Locale.ROOT).contains(term)).toList();
        return "<div class=\"page-intro\"><div><span class=\"eyebrow\">DISCOVER SGU</span><h1>Địa điểm tham quan</h1><p>Tìm một điểm đến, mở ra một câu chuyện.</p></div>"+link("/map",icon("map")+"Xem trên bản đồ","btn btn-light")+"</div>"+search("/places",q,"Tìm tên địa điểm, mã POI hoặc khu vực...")+"<div class=\"count\">"+shown.size()+" địa điểm đang hiển thị</div><div class=\"cards\">"+cards(shown)+"</div>";
    }
    public static String search(String action,String q,String placeholder) {
        return "<form class=\"search-bar\" method=\"get\" action=\""+e(action)+"\">"+icon("search")+"<input name=\"q\" aria-label=\"Tìm kiếm\" value=\""+e(q)+"\" placeholder=\""+e(placeholder)+"\"><button class=\"btn\">Tìm kiếm</button></form>";
    }
    public String detail(Map<String,Object> poi,List<Map<String,Object>> languages,Map<String,Object> narration,Map<String,Object> audio,String lang,boolean preview,String audioError) {
        long id=Data.id(poi,"id");
        StringBuilder pills=new StringBuilder("<form class=\"language-pills\" action=\"/place\" method=\"get\">"+hidden("id",id)+(preview?hidden("preview","1"):""));
        for(var l:languages) {
            String code=Data.text(l,"languageCode");
            pills.append("<button type=\"submit\" name=\"lang\" value=\"").append(e(code)).append("\" class=\"").append(code.equals(lang)?"active":"").append("\">").append(code.equals(lang)?icon("check"):icon("globe")).append(e(l.get("languageName"))).append("</button>");
        }
        pills.append("</form>");
        String title=narration==null?"Chưa có bài thuyết minh":Data.text(narration,"title");
        StringBuilder track=new StringBuilder();
        if(audio!=null) {
            track.append("<div class=\"wave\" aria-hidden=\"true\">");
            int[] heights= {
                9,14,20,32,22,40,51,31,19,36,48,26,43,53,33,25,41,29,18,37,46,24,14,26,17,9
            };
            for(int h:heights)track.append("<i style=\"--h:").append(h).append("px\"></i>");
            track.append("</div><div class=\"track-name\">").append(e(title)).append("</div><p class=\"track-meta\">").append(seconds(audio.get("durationSeconds"))).append(" · MP3 · Phiên bản ").append(e(audio.get("versionNumber"))).append("</p><audio controls preload=\"metadata\" src=\"/media/audio/").append(Data.id(audio,"id")).append(preview?"?preview=1":"").append("\" aria-label=\"Nghe thuyết minh\">Trình duyệt của bạn chưa hỗ trợ phát audio.</audio><p class=\"player-note\">Bấm nút phát để nghe. Có thể tạm dừng và kéo thanh thời gian ngay trong ứng dụng.</p>");
        } else {
            track.append("<div class=\"tip\">").append(icon("audio")).append("<span>").append(e(audioError==null||audioError.isBlank()?(narration==null?"Chưa có nội dung bằng ngôn ngữ này. Hãy chọn ngôn ngữ khác.":"Nội dung này chưa được gắn MP3. Quản trị viên có thể tải file trong Nội dung & audio."):audioError)).append("</span></div>");
        } String desc=narration==null?Data.text(poi,"description"):Data.text(narration,"content");
        return "<div class=\"crumb\">"+link("/","Khám phá","")+" / "+link("/places","Địa điểm","")+" / <span>"+e(poi.get("poiName"))+"</span></div>"+(preview?"<div class=\"notice\">Bạn đang nghe thử với quyền quản trị. Không ghi lượt nghe của khách.</div>":"")+"<div class=\"detail-grid\"><div class=\"detail-main\"><div class=\"detail-picture\"><img src=\""+picture(poi)+"\" alt=\""+e(poi.get("poiName"))+"\"><div class=\"picture-caption\"><span class=\"tag\">"+icon("pin")+e(poi.get("areaName"))+"</span><h1>"+e(poi.get("poiName"))+"</h1><p>"+e(poi.get("poiCode"))+" · Saigon Audio Guide</p></div>"+imageLabel(poi)+"</div><article class=\"detail-text\"><span class=\"eyebrow\">THE STORY BEHIND</span><h2>"+e(narration==null?"Giới thiệu":title)+"</h2><p>"+e(desc)+"</p>"+link("/map?id="+id,icon("map")+"Xem vị trí & mô phỏng GPS","btn btn-light")+"</article></div><aside class=\"player-card\"><div class=\"player-heading\">"+icon("audio")+"<div><h2>Nghe thuyết minh</h2><p>Câu chuyện được kể theo cách của bạn</p></div></div><div class=\"language-label\">CHỌN NGÔN NGỮ</div>"+pills+track+"<hr class=\"player-separator\"><div class=\"tip\">"+icon("pin")+"<span>Địa điểm được chọn ở chế độ mô phỏng. Không yêu cầu truy cập vị trí của bạn.</span></div>"+(s.demo()?"<p class=\"player-note\" style=\"margin-top:14px\">Giọng đọc tổng hợp dùng cho demo; không phải nội dung chính thức của trường.</p>":"")+"</aside></div>";
    }
    public String map(ClientSessionDTO ss,List<Map<String,Object>> pois,long selected,boolean osm) {
        if(pois.isEmpty())return "<div class=\"empty\"><h1>Chưa có POI trên bản đồ</h1></div>";
        Map<String,Object> active=pois.stream().filter(m->Data.id(m,"id")==selected).findFirst().orElse(pois.getFirst());
        long id=Data.id(active,"id");
        StringBuilder list=new StringBuilder("<div class=\"map-list\">");
        for(int i=0;i<pois.size();i++) {
            var p=pois.get(i);
            list.append(link("/map?id="+Data.id(p,"id")+(osm?"&basemap=osm":""),"<span>"+(i+1)+"</span><div><strong>"+e(p.get("poiName"))+"</strong><small>"+e(p.get("poiCode"))+" · Bán kính "+e(p.get("triggerRadiusM"))+" m</small></div>",Data.id(p,"id")==id?"active":""));
        }
        list.append("</div>");
        double lat=Data.number(active,"latitude"),lon=Data.number(active,"longitude");
        String drawing;
        if(osm) {
            String url="https://www.openstreetmap.org/export/embed.html?bbox="+(lon-.002)+"%2C"+(lat-.0015)+"%2C"+(lon+.002)+"%2C"+(lat+.0015)+"&layer=mapnik&marker="+lat+"%2C"+lon;
            drawing="<iframe class=\"map-iframe\" title=\"Bản đồ OpenStreetMap\" loading=\"lazy\" src=\""+e(url)+"\"></iframe>";
        } else {
            double minLat=pois.stream().mapToDouble(m->Data.number(m,"latitude")).min().orElse(lat)-.0004,maxLat=pois.stream().mapToDouble(m->Data.number(m,"latitude")).max().orElse(lat)+.0004,minLon=pois.stream().mapToDouble(m->Data.number(m,"longitude")).min().orElse(lon)-.0004,maxLon=pois.stream().mapToDouble(m->Data.number(m,"longitude")).max().orElse(lon)+.0004;
            StringBuilder svg=new StringBuilder("<svg viewBox=\"0 0 760 460\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-label=\"Sơ đồ tọa độ POI. Bấm từng điểm để mô phỏng.\"><defs><pattern id=\"grid\" width=\"48\" height=\"48\" patternUnits=\"userSpaceOnUse\"><path d=\"M48 0H0V48\" fill=\"none\" stroke=\"#c5dcea\" stroke-width=\"1\"/></pattern></defs><rect width=\"760\" height=\"460\" fill=\"url(#grid)\"/><rect x=\"25\" y=\"22\" width=\"225\" height=\"48\" rx=\"13\" fill=\"white\"/><text x=\"42\" y=\"51\" font-size=\"12\" font-family=\"Arial\" fill=\"#2c628b\">Sơ đồ tọa độ · Mô phỏng</text>");
            for(var p:pois) {
                double x=70+(Data.number(p,"longitude")-minLon)/(maxLon-minLon)*620,y=390-(Data.number(p,"latitude")-minLat)/(maxLat-minLat)*290;
                String px=String.format(Locale.ROOT,"%.2f",x),py=String.format(Locale.ROOT,"%.2f",y);
                boolean on=Data.id(p,"id")==id;
                svg.append("<a href=\"/map?id=").append(Data.id(p,"id")).append("\"><title>").append(e(p.get("poiName"))).append("</title>");
                if(on)svg.append("<circle cx=\"").append(px).append("\" cy=\"").append(py).append("\" r=\"33\" fill=\"#149aea22\"/>");
                svg.append("<circle class=\"map-pin ").append(on?"active":"").append("\" cx=\"").append(px).append("\" cy=\"").append(py).append("\" r=\"14\"/><text class=\"map-label\" text-anchor=\"middle\" x=\"").append(px).append("\" y=\"").append(String.format(Locale.ROOT,"%.2f",y+35)).append("\">").append(e(p.get("poiName"))).append("</text></a>");
            }
            drawing=svg.append("</svg>").toString();
        } return "<div class=\"page-intro\"><div><span class=\"eyebrow\">FIND YOUR WAY</span><h1>Bản đồ khám phá</h1><p>Chọn POI để mô phỏng vị trí và mở bài thuyết minh.</p></div>"+link("/map?id="+id+(osm?"":"&basemap=osm"),icon("map")+(osm?"Sơ đồ tọa độ":"Bản đồ OpenStreetMap"),"btn btn-light")+"</div><div class=\"map-grid\"><div><div class=\"map-board\">"+drawing+"<div class=\"map-legend\"><span>"+(osm?"© OpenStreetMap contributors":"Sơ đồ tọa độ, không phải bản đồ đường đi")+"</span><span>"+e(lat)+", "+e(lon)+"</span></div></div><p class=\"map-disclaimer\">Bản đồ nền OpenStreetMap cần Internet. Sơ đồ tọa độ vẫn hoạt động khi không tải được bản đồ nền.</p><section class=\"panel\"><h3>Mô phỏng GPS bằng tọa độ</h3><p class=\"help\">Chỉ chọn POI nếu tọa độ nằm trong bán kính kích hoạt của địa điểm.</p><form method=\"post\" action=\"/simulate\">"+hidden("csrf",ss.csrf)+"<div class=\"form-grid\">"+input("Vĩ độ","latitude",lat,"number","required min=\"-90\" max=\"90\" step=\"0.0000001\"")+input("Kinh độ","longitude",lon,"number","required min=\"-180\" max=\"180\" step=\"0.0000001\"")+"</div><div class=\"form-actions\"><button class=\"btn\">"+icon("pin")+"Tìm POI phù hợp</button></div></form></section></div><aside class=\"panel\"><h2>Chọn điểm dừng</h2><p class=\"help\">"+pois.size()+" địa điểm trong danh sách</p>"+list+"<div class=\"tip\">"+icon("pin")+"<span>Đang mô phỏng tại <strong>"+e(active.get("poiName"))+"</strong>.</span></div><div class=\"form-actions\">"+link("/place?id="+id,icon("audio")+"Nghe thuyết minh","btn")+"</div></aside></div>";
    }
}
