package com.sgu.gateway.DAO;
import com.sgu.core.*;
import com.sgu.gateway.DTO.*;
import java.util.*;
/** File-backed demo adapter, never selected as a fallback from a failed real API. */
public final class DemoCatalogDAO {
    private final JsonStore store;
    private final List<Map<String,Object>> rows;
    public DemoCatalogDAO(Settings s)throws Exception {
        store=new JsonStore(s.data("catalog").resolve("pois.json"));
        rows=store.read(List.of( new POIDTO(1,"SGU-001","Cổng trường",1,"Khuôn viên SGU","Dữ liệu minh họa: bắt đầu hành trình khám phá không gian đại học. Thay bằng nội dung và tọa độ khảo sát của nhóm khi dùng thật.",10.75995,106.68205,50,"",true).map(), new POIDTO(2,"SGU-002","Thư viện",1,"Khuôn viên SGU","Dữ liệu minh họa: không gian đọc sách và học tập. Ảnh và tọa độ demo chưa được khảo sát.",10.76025,106.68225,35,"",true).map(), new POIDTO(3,"SGU-003","Giảng đường",1,"Khuôn viên SGU","Dữ liệu minh họa cho trang chi tiết địa điểm và nội dung thuyết minh.",10.76042,106.68253,40,"",true).map() ));
    }
    public synchronized List<Map<String,Object>> list(boolean admin) {
        return rows.stream().filter(m->admin||Data.bool(m,"visible",true)).map(LinkedHashMap::new).map(m->(Map<String,Object>)m).toList();
    }
    public List<Map<String,Object>> areas() {
        return List.of(Data.row("id",1,"areaCode","SGU","areaName","Khuôn viên SGU","visible",true));
    }
    public synchronized long save(long id,Map<String,Object> input)throws Exception {
        final long checkId=id;
        String code=Data.text(input,"poiCode");
        if(rows.stream().anyMatch(m->Data.id(m,"id")!=checkId&&Data.text(m,"poiCode").equalsIgnoreCase(code)))throw new Http.Error(409,"Mã POI đã tồn tại.");
        if(Data.id(input,"areaId")!=1)throw new Http.Error(400,"Khu vực không tồn tại.");
        Map<String,Object> value=new LinkedHashMap<>(input);
        value.put("areaName","Khuôn viên SGU");
        value.put("imageUrl","");
        if(id==0) {
            id=JsonStore.next(rows);
            value.put("visible",true);
            value.put("id",id);
            rows.add(value);
        }
        else {
            Map<String,Object> old=find(id);
            value.put("id",id);
            value.put("visible",Data.bool(old,"visible",true));
            rows.set(rows.indexOf(old),value);
        }
        store.write(rows);
        return id;
    }
    public synchronized void visibility(long id,boolean value)throws Exception {
        find(id).put("visible",value);
        store.write(rows);
    }
    public synchronized Map<String,Object> find(long id) {
        return rows.stream().filter(m->Data.id(m,"id")==id).findFirst().orElseThrow(()->new Http.Error(404,"Không tìm thấy POI."));
    }
}
