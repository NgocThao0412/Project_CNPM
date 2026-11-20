package web.view;

import core.Data;
import web.DTO.ClientSessionDTO;
import java.util.*;
import static web.view.Html.*;

/** Event-route list, sequential guidance and admin editor. Keeps original map/POI UI untouched. */
public final class EventRouteView {
    private static int stepsCount(Map<String,Object> route){
        Object raw=route.get("stops");
        return raw instanceof List<?> rows ? rows.size() : 0;
    }
    private static String val(Map<String,Object> m,String k){return Data.text(m,k);}
    private static String toggle(Map<String,Object> m,String name){return Data.bool(m,name,false)?" checked":"";}
    private static String item(String name,Object value,String type,String extra){
        return input(name,name,value,type,extra);
    }
    public String publicUnavailable(){
        return "<section class=\"event-unavailable\" aria-labelledby=\"event-unavailable-title\">"
            +"<h1 id=\"event-unavailable-title\">Lộ trình chưa được công bố</h1>"
            +"<p>Lộ trình này hiện đang tạm ẩn và chưa mở cho sinh viên tham quan."
            +" Vui lòng quay lại trang tham quan để lựa chọn các lộ trình hiện có.</p>"
            +link("/","← Trở về trang tham quan","btn")
            +"</section>";
    }
    public String publicList(List<Map<String,Object>> routes){
        StringBuilder b=new StringBuilder("<div class=\"page-intro\"><div>"
          +"<h1>Đường đi cho tân sinh viên</h1><p>Xem thứ tự các bước nhập học, khám sức khỏe và sinh hoạt đầu khóa.</p>"
          +"</div>"+(routes.isEmpty()?"":link("/map",icon("map")+"Xem bản đồ","btn btn-light"))+"</div>"
          +"<section class=\"event-cards\">");
        for(var route:routes){
            long id=Data.id(route,"id");
            b.append("<article class=\"event-card\">")
             .append("<h2>").append(e(val(route,"title"))).append("</h2><p>")
             .append(e(val(route,"summary"))).append("</p><div class=\"event-card-bottom\"><small>")
             .append(e(route.get("stopCount"))).append(" bước · ").append(e(val(route,"audience")))
             .append("</small>").append(link("/event-route?id="+id,"Xem luồng đường đi "+icon("arrow"),"btn"))
             .append("</div></article>");
        }
        if(routes.isEmpty())b.append("<div class=\"empty\"><h2>Chưa có lộ trình nào được thêm</h2><p>Hiện chưa có lộ trình để hiển thị cho sinh viên.</p>"+link("/","← Trở về trang chủ","btn")+"</div>");
        return b.append("</section>").toString();
    }
    public String publicDetail(Map<String,Object> route){
        StringBuilder b=new StringBuilder("<div class=\"crumb\">"+link("/","Khám phá"," ")+" / "+e(val(route,"title"))+"</div>"
           +"<div class=\"event-detail-head\"><h1>"+e(val(route,"title"))+"</h1>"
           +"<p>"+e(val(route,"summary"))+"</p></div>"
           +"<section class=\"event-steps\" aria-label=\"Các bước thực hiện\">");
        List<Map<String,Object>> stops=Data.list(route.get("stops"));
        int count=0;
        for(var s:stops){
            count++;
            String place=val(s,"locationLabel");
            long poiId=0;
            try{poiId=Long.parseLong(val(s,"poiId"));}catch(Exception ignored){}
            b.append("<article class=\"event-step\" id=\"event-step-").append(count)
             .append("\"><div class=\"event-step-index\">").append(count)
             .append("</div><div class=\"event-step-body\"><h2>").append(e(val(s,"title")))
             .append("</h2><p>").append(e(val(s,"instructions"))).append("</p><div class=\"event-step-footer\"><span>")
             .append(icon("pin")).append(" ").append(e(place.isBlank()?"Địa điểm theo thông báo SGU":place))
             .append("</span>");
            if(Data.number(s,"durationMinutes")>0)b.append("<span>").append(icon("clock")).append(" Khoảng ").append(e(val(s,"durationMinutes"))).append(" phút (ước tính)</span>");
            if(poiId>0)b.append(link("/map?id="+poiId+"&basemap=osm",icon("map")+"Xem POI trên bản đồ","btn btn-light btn-small"));
            else b.append("<small class=\"event-waiting\">Chưa gắn vị trí trên bản đồ</small>");
            b.append("</div></div></article>");
        }
        return b.append("</section><div class=\"event-end-actions\">"+link("/","← Về trang Khám phá","btn btn-light")+"</div>").toString();
    }
    public String adminList(ClientSessionDTO session,List<Map<String,Object>> routes){
        StringBuilder b=new StringBuilder("<div class=\"section-header\"><div><h2>Quản lý đường đi sự kiện</h2></div>"
             +link("/admin/event-route",icon("plus")+"Tạo lộ trình","btn")+"</div>"
             +"<section class=\"panel\"><div class=\"table-wrap\"><table class=\"data-table\"><thead><tr><th>Lộ trình</th><th>Số bước</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>");
        for(var r:routes){
            long id=Data.id(r,"id");
            boolean publicNow=Data.bool(r,"published",false) && !Data.bool(r,"template",false);
            boolean draft=!publicNow && Data.bool(r,"template",false);
            String status=publicNow?"Đang công khai":draft?"Bản nháp":"Đã ẩn";
            String action=publicNow?"Ẩn":draft?"Công khai":"Công khai lại";
            b.append("<tr><td><strong>").append(e(val(r,"title"))).append("</strong><br><small>").append(e(val(r,"routeCode"))).append("</small></td>"
             +"<td>").append(e(r.get("stopCount"))).append("</td><td>").append(status)
             .append("</td><td><div class=\"row-actions\">")
             .append(link("/admin/event-route?id="+id,"Chỉnh sửa","btn btn-light btn-small"))
             .append("<form class=\"inline-form\" method=\"post\" action=\"/admin/event-route/visibility\">")
             .append(hidden("csrf",session.csrf)).append(hidden("id",id))
             .append("<input type=\"hidden\" name=\"published\" value=\"")
             .append(publicNow?"false":"true").append("\">")
             .append("<button class=\"btn ").append(publicNow?"btn-light btn-small":"btn-small")
             .append("\" type=\"submit\">").append(action).append("</button></form>")
             .append("</div></td></tr>");
        }
        if(routes.isEmpty())b.append("<tr><td colspan=\"4\">Chưa có lộ trình.</td></tr>");
        return b.append("</tbody></table></div></section>").toString();
    }
    private static String value(Map<String,Object> form,String key){return e(val(form,key));}
    public String adminForm(ClientSessionDTO session,Map<String,Object> route,List<Map<String,Object>> pois,String error){
        long id=0;
        try{id=Long.parseLong(val(route,"id"));}catch(Exception ignored){}
        boolean create=id==0;
        boolean publicNow=!create && Data.bool(route,"published",false) && !Data.bool(route,"template",false);
        boolean draft=create || Data.bool(route,"template",false);
        StringBuilder b=new StringBuilder("<div class=\"section-header\"><div><h2>"
          +(create?"Tạo lộ trình":"Chỉnh sửa lộ trình")
          +"</h2><p>Thứ tự bên dưới là thứ tự sinh viên thực hiện các bước.</p></div>"
          +"</div>");
        if(error!=null&&!error.isBlank())b.append("<div class=\"error-box\"><p>").append(e(error)).append("</p></div>");
        b.append("<form action=\"/admin/event-route/save\" method=\"post\" class=\"panel event-editor\" data-sgu-route-status=\""
          +(publicNow?"published":draft?"draft":"hidden")+"\">"
          +hidden("csrf",session.csrf)+hidden("id",id)
          +"<div class=\"form-grid\">"
          +input("Mã lộ trình","routeCode",val(route,"routeCode"),"text","required maxlength=\"60\" pattern=\"[A-Za-z0-9_-]{3,60}\"")
          +input("Tên lộ trình","title",val(route,"title"),"text","required maxlength=\"180\"")
          +hidden("audience",val(route,"audience").isBlank()?"Tân sinh viên":val(route,"audience"))
          +"</div><label class=\"field\"><span>Mô tả</span><textarea name=\"summary\" maxlength=\"2000\" rows=\"3\" required>"
          +value(route,"summary")+"</textarea></label>"
                    +"<section class=\"sgu-route-step-panel\" aria-label=\"Các bước trong lộ trình\">"
          +"<div class=\"sgu-route-step-heading\"><h3>Các bước trong lộ trình</h3>"
          +"<span class=\"sgu-route-step-count\" id=\"sgu-route-step-count\">Tổng số bước: "+Math.min(stepsCount(route),12)+"</span></div>"
          +"<div class=\"sgu-route-step-list\" id=\"sgu-route-step-list\">");
        List<Map<String,Object>> steps=new ArrayList<>();
        Object raw=route.get("stops");
        if(raw instanceof List<?>)steps.addAll(Data.list(raw));
        for(int i=1;i<=12;i++){
            Map<String,Object> step=i<=steps.size()?steps.get(i-1):Map.of();
            String title=val(step,"title"), location=val(step,"locationLabel"), instructions=val(step,"instructions");
            b.append("<details class=\"sgu-route-step-card\" data-sgu-route-step data-slot=\"").append(i).append("\"");
            if(i>steps.size())b.append(" hidden");
            b.append("><summary class=\"sgu-route-step-bar\">"
               +"<span class=\"sgu-route-step-grip\" draggable=\"true\" title=\"Kéo để đổi thứ tự bước\" aria-hidden=\"true\">⠿</span>"
               +"<span class=\"sgu-route-step-number\" data-sgu-route-number>").append(i).append("</span>"
               +"<strong class=\"sgu-route-step-label\" data-sgu-route-label>")
               .append(e(title.isBlank()?"Bước "+i+": Chưa nhập":"Bước "+i+": "+title))
               .append("</strong><span class=\"sgu-route-step-actions\">"
               +"<button type=\"button\" class=\"sgu-route-icon-btn\" data-sgu-route-edit title=\"Chỉnh sửa bước\" aria-label=\"Chỉnh sửa bước\">"
               +icon("edit")+"</button>"
               +"<button type=\"button\" class=\"sgu-route-icon-btn sgu-route-remove-btn\" data-sgu-route-remove title=\"Xóa bước\" aria-label=\"Xóa bước\">"
               +"<svg class=\"icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">"
               +"<path d=\"M3 6h18 M9 6V4h6v2 M6 6l1 15h10l1-15 M10 10v7 M14 10v7\"/></svg></button>"
               +"</span></summary><div class=\"sgu-route-step-fields\"><div class=\"form-grid\">"
               +input("Tên bước","stopTitle"+i,title,"text","maxlength=\"180\"")
               +input("Khu vực / phòng (nếu có)","stopLocation"+i,location,"text","maxlength=\"200\"")
               +"<label class=\"field\"><span>Gắn địa điểm POI (không bắt buộc)</span><select name=\"stopPoi"+i+"\"><option value=\"0\">Chưa xác nhận POI</option>");
            for(var poi:pois){
                String poiId=val(poi,"id");
                b.append("<option value=\"").append(e(poiId)).append("\"")
                 .append(poiId.equals(val(step,"poiId"))?" selected":"")
                 .append(">").append(e(val(poi,"poiName"))).append("</option>");
            }
            b.append("</select></label>"
                +input("Thời lượng tham khảo (phút)","stopMinutes"+i,val(step,"durationMinutes").isBlank()?0:val(step,"durationMinutes"),"number","min=\"0\" max=\"240\"")
                +"</div><label class=\"field\"><span>Hướng dẫn thực hiện</span><textarea rows=\"3\" maxlength=\"3000\" name=\"stopInstructions"+i+"\">"
                +e(instructions)+"</textarea></label></div></details>");
        }
        String saveActions;
        if(draft){
            saveActions="<button class=\"btn btn-light sgu-route-draft-btn\" type=\"submit\" name=\"saveMode\" value=\"draft\">Lưu bản nháp</button>"
                +"<button class=\"btn\" type=\"submit\" name=\"saveMode\" value=\"publish\">Lưu và công khai</button>";
        } else if(publicNow){
            saveActions="<button class=\"btn\" type=\"submit\" name=\"saveMode\" value=\"update\">Lưu thay đổi</button>";
        } else {
            saveActions="<button class=\"btn btn-light\" type=\"submit\" name=\"saveMode\" value=\"update\">Lưu thay đổi</button>"
                +"<button class=\"btn\" type=\"submit\" name=\"saveMode\" value=\"publish\">Công khai</button>";
        }
        b.append("</div><button type=\"button\" class=\"sgu-route-add-step\" id=\"sgu-route-add-step\">"
            +icon("plus")+" Thêm bước</button>"
            +"</section>"
            +"<div class=\"sgu-route-save-row\"><div class=\"sgu-route-save-buttons\">"
            +saveActions
            +link("/admin/event-routes","Hủy","btn btn-light")
            +"</div></div></form>"
            +"<link rel=\"stylesheet\" href=\"/assets/sgu-route-editor.css\">"
            +"<script defer src=\"/assets/sgu-route-editor.js\"></script>");
        return b.toString();
    }
}
