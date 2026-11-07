package web.view;

import core.Data;
import web.DTO.ClientSessionDTO;
import java.util.*;
import static web.view.Html.*;

/** Event-route list, sequential guidance and admin editor. Keeps original map/POI UI untouched. */
public final class EventRouteView {
    private static String val(Map<String,Object> m,String k){return Data.text(m,k);}
    private static String toggle(Map<String,Object> m,String name){return Data.bool(m,name,false)?" checked":"";}
    private static String item(String name,Object value,String type,String extra){
        return input(name,name,value,type,extra);
    }
    public String publicList(List<Map<String,Object>> routes){
        StringBuilder b=new StringBuilder("<div class=\"page-intro\"><div>"
          +"<h1>Đường đi cho tân sinh viên</h1><p>Xem thứ tự các bước nhập học, khám sức khỏe và sinh hoạt đầu khóa.</p>"
          +"</div>"+link("/map",icon("map")+"Xem bản đồ","btn btn-light")+"</div>"
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
        if(routes.isEmpty())b.append("<div class=\"empty\"><h2>Chưa có lộ trình được công bố</h2><p>Quản trị viên có thể tạo và công bố tại mục Đường đi sự kiện.</p></div>");
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
    public String adminList(List<Map<String,Object>> routes){
        StringBuilder b=new StringBuilder("<div class=\"section-header\"><div><h2>Quản lý đường đi sự kiện</h2><p>Tạo luồng nhập học, khám sức khỏe và gắn các bước vào POI.</p></div>"
             +link("/admin/event-route",icon("plus")+"Tạo lộ trình","btn")+"</div>"
             +"<div class=\"event-note\">Các tuyến mẫu chỉ mang tính tham khảo. Hãy cập nhật địa điểm, lịch tổ chức và xác nhận trước khi bỏ trạng thái mẫu.</div>"
             +"<section class=\"panel\"><div class=\"table-wrap\"><table class=\"data-table\"><thead><tr><th>Lộ trình</th><th>Số bước</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>");
        for(var r:routes){
            long id=Data.id(r,"id");
            b.append("<tr><td><strong>").append(e(val(r,"title"))).append("</strong><br><small>").append(e(val(r,"routeCode"))).append("</small></td>"
             +"<td>").append(e(r.get("stopCount"))).append("</td><td>")
             .append(Data.bool(r,"published",false)?"Đang công bố":"Bản nháp")
             .append(Data.bool(r,"template",false)?" · Mẫu":"")
             .append("</td><td>").append(link("/admin/event-route?id="+id,"Chỉnh sửa","btn btn-light btn-small"))
             .append("</td></tr>");
        }
        if(routes.isEmpty())b.append("<tr><td colspan=\"4\">Chưa có lộ trình.</td></tr>");
        return b.append("</tbody></table></div></section>").toString();
    }
    private static String value(Map<String,Object> form,String key){return e(val(form,key));}
    public String adminForm(ClientSessionDTO session,Map<String,Object> route,List<Map<String,Object>> pois,String error){
        long id=0;
        try{id=Long.parseLong(val(route,"id"));}catch(Exception ignored){}
        boolean create=id==0;
        StringBuilder b=new StringBuilder("<div class=\"section-header\"><div><h2>"
          +(create?"Tạo lộ trình":"Chỉnh sửa lộ trình")
          +"</h2><p>Thứ tự bên dưới là thứ tự sinh viên thực hiện các bước.</p></div>"
          +link("/admin/event-routes","← Danh sách","btn btn-light")+"</div>");
        if(error!=null&&!error.isBlank())b.append("<div class=\"error-box\"><p>").append(e(error)).append("</p></div>");
        b.append("<form action=\"/admin/event-route/save\" method=\"post\" class=\"panel event-editor\">"
          +hidden("csrf",session.csrf)+hidden("id",id)
          +"<div class=\"form-grid\">"
          +input("Mã lộ trình","routeCode",val(route,"routeCode"),"text","required maxlength=\"60\" pattern=\"[A-Za-z0-9_-]{3,60}\"")
          +input("Tên lộ trình","title",val(route,"title"),"text","required maxlength=\"180\"")
          +input("Đối tượng","audience",val(route,"audience").isBlank()?"Tân sinh viên":val(route,"audience"),"text","required maxlength=\"100\"")
          +"</div><label class=\"field\"><span>Mô tả</span><textarea name=\"summary\" maxlength=\"2000\" rows=\"3\" required>"
          +value(route,"summary")+"</textarea></label>"
          +"<div class=\"event-editor-flags\"><label><input type=\"checkbox\" name=\"published\" value=\"true\""+toggle(route,"published")+"> Công bố cho sinh viên</label>"
          +"<label><input type=\"checkbox\" name=\"template\" value=\"true\""+(create?" checked":toggle(route,"template"))
          +"> Đây là lộ trình mẫu, chưa được xác minh</label></div>"
          +"<h3>Các bước trong lộ trình</h3><p class=\"help\">Nhập liên tiếp từ bước 1, tối đa 12 bước; bỏ trống tên bước để không lưu. Chọn POI thật nếu đã xác nhận vị trí.</p>");
        List<Map<String,Object>> steps=new ArrayList<>();
        Object raw=route.get("stops");
        if(raw instanceof List<?>)steps.addAll(Data.list(raw));
        for(int i=1;i<=12;i++){
            Map<String,Object> step=i<=steps.size()?steps.get(i-1):Map.of();
            String title=val(step,"title"), location=val(step,"locationLabel"), instructions=val(step,"instructions");
            b.append("<details class=\"event-editor-step\" ").append(i<=Math.max(1,steps.size())?"open":"").append(">"
               +"<summary>Bước "+i+(title.isBlank()?" · Chưa nhập":" · "+e(title))+"</summary>"
               +"<div class=\"form-grid\">"
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
                +e(instructions)+"</textarea></label></details>");
        }
        b.append("<div class=\"form-actions\"><button class=\"btn\" type=\"submit\">Lưu lộ trình</button>"
          +link("/admin/event-routes","Hủy","btn btn-light")+"</div></form>");
        if(!create)b.append("<form class=\"event-delete\" method=\"post\" action=\"/admin/event-route/delete\">"
          +hidden("csrf",session.csrf)+hidden("id",id)
          +"<button class=\"btn btn-light\" type=\"submit\">Xóa lộ trình</button></form>");
        return b.toString();
    }
}
