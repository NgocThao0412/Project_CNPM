package BUS;

import DAO.EventRouteDAO;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class EventRouteBUS {
    private final EventRouteDAO dao;
    public EventRouteBUS(EventRouteDAO dao){this.dao=dao;}
    public List<Map<String,Object>> all(boolean admin){return dao.all(admin);}
    public Map<String,Object> one(long id,boolean admin){
        Map<String,Object> result=dao.one(id,admin);
        if(result==null)throw new NoSuchElementException("Không tìm thấy tuyến đường.");
        return result;
    }
    private String str(Map<String,Object> m,String field,int max,boolean required){
        String text=Objects.toString(m.get(field),"").trim();
        if(text.length()>max||(required&&text.isBlank()))
            throw new IllegalArgumentException("Trường "+field+" không hợp lệ (tối đa "+max+" ký tự).");
        return text;
    }
    private boolean yes(Map<String,Object> m,String key){
        return Boolean.TRUE.equals(m.get(key)) || "true".equalsIgnoreCase(Objects.toString(m.get(key),""));
    }
    public long save(long id,Map<String,Object> form){
        if(id<0)throw new IllegalArgumentException("Mã tuyến không hợp lệ.");
        String code=str(form,"routeCode",60,true);
        if(!code.matches("[A-Za-z0-9_-]{3,60}"))throw new IllegalArgumentException("Mã tuyến chỉ gồm chữ, số, gạch ngang và gạch dưới.");
        Map<String,Object> route=new LinkedHashMap<>();
        route.put("routeCode",code);route.put("title",str(form,"title",180,true));
        route.put("summary",str(form,"summary",2000,true));route.put("audience",str(form,"audience",100,true));
        route.put("published",yes(form,"published"));route.put("template",yes(form,"template"));
        Object raw=form.get("stops");
        if(!(raw instanceof List<?> all)||all.isEmpty()||all.size()>12)
            throw new IllegalArgumentException("Mỗi tuyến cần từ 1 đến 12 bước.");
        List<Map<String,Object>> stops=new ArrayList<>();
        for(Object o:all){
            if(!(o instanceof Map<?,?> x))throw new IllegalArgumentException("Thông tin bước không hợp lệ.");
            Map<String,Object> m=new LinkedHashMap<>();x.forEach((k,v)->m.put(Objects.toString(k),v));
            String title=str(m,"title",180,true);
            String instructions=str(m,"instructions",3000,true);
            String place=str(m,"locationLabel",200,false);
            long poi=0;
            try{poi=Long.parseLong(Objects.toString(m.get("poiId"),"0"));}
            catch(NumberFormatException ex){throw new IllegalArgumentException("POI không hợp lệ.");}
            if(poi<0||(poi>0&&!dao.poiExists(poi)))throw new IllegalArgumentException("POI không tồn tại.");
            int minutes=0;
            try{minutes=Integer.parseInt(Objects.toString(m.get("durationMinutes"),"0"));}
            catch(NumberFormatException ex){throw new IllegalArgumentException("Thời lượng không hợp lệ.");}
            if(minutes<0||minutes>240)throw new IllegalArgumentException("Mỗi bước phải từ 0 đến 240 phút.");
            Map<String,Object> step=new LinkedHashMap<>();
            step.put("title",title);
            step.put("instructions",instructions);
            step.put("locationLabel",place);
            step.put("poiId",poi==0?null:poi);
            step.put("durationMinutes",minutes);
            stops.add(step);
        }
        return dao.save(id,route,stops);
    }
    public void delete(long id){one(id,true);dao.delete(id);}
}
