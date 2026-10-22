package com.sgu.narration.DAO;
import com.sgu.core.*;
import java.util.*;
import java.sql.*;
public final class NarrationDAO {
    private final Settings settings;
    private final Database db;
    private final JsonStore store;
    private final List<Map<String,Object>> rows;
    private static final String SELECT="SELECT n.id,n.poi_id AS poiId,n.language_id AS languageId,l.language_code AS languageCode,l.language_name AS languageName,n.title,n.content,n.is_visible AS visible FROM narration_contents n JOIN languages l ON l.id=n.language_id JOIN points_of_interest p ON p.id=n.poi_id JOIN campus_areas ca ON ca.id=p.area_id ";
    public NarrationDAO(Settings s)throws Exception {
        settings=s;
        db=new Database(s);
        if(s.demo()) {
            store=new JsonStore(s.data("narration").resolve("contents.json"));
            String vi="Chào mừng bạn đến với Saigon Audio Guide. Đây là bản thuyết minh mẫu để kiểm tra chức năng phát âm thanh. Hãy chọn một địa điểm, chọn ngôn ngữ và bấm nút phát để bắt đầu trải nghiệm. Nội dung này là dữ liệu minh họa, không phải bài giới thiệu chính thức của nhà trường.";
            rows=store.read(List.of( seed(1,1,1,"vi","Tiếng Việt","Lời chào từ SGU",vi), seed(2,1,2,"en","English","Welcome to Saigon Audio Guide","Welcome to Saigon Audio Guide. This is a sample narration to test the audio player. Select a place, choose a language, and press play to start exploring. This demonstration is not an official description of the university."), seed(3,2,1,"vi","Tiếng Việt","Không gian tri thức","Nội dung minh họa cho thư viện. Bạn có thể thêm MP3 tương ứng trong trang Quản lý nội dung."), seed(4,3,1,"vi","Tiếng Việt","Một ngày ở giảng đường","Nội dung minh họa cho giảng đường. Thay bằng bài thuyết minh đã được nhóm duyệt.") ));
        }
        else {
            store=null;
            rows=null;
        }
    } private static Map<String,Object> seed(long id,long poi,long lang,String code,String name,String title,String content) {
        return Data.row("id",id,"poiId",poi,"languageId",lang,"languageCode",code,"languageName",name,"title",title,"content",content,"visible",true);
    }
    public List<Map<String,Object>> languages()throws Exception {
        if(settings.demo())return List.of(Data.row("id",1,"languageCode","vi","languageName","Tiếng Việt"),Data.row("id",2,"languageCode","en","languageName","English"));
        try(Connection c=db.open()) {
            return Database.rows(c,"SELECT id,language_code AS languageCode,language_name AS languageName FROM languages WHERE is_active=1 ORDER BY id");
        }
        catch(SQLException e) {
            throw Database.friendly(e);
        }
    }
    public synchronized List<Map<String,Object>> list(boolean admin)throws Exception {
        if(settings.demo()) {
            Set<Long> visible=new HashSet<>();
            if(!admin)for(var p:Data.list(Http.call("GET",settings.poi()+"/api/pois","",null).require().json()))visible.add(Data.id(p,"id"));
            return rows.stream().filter(m->admin||(Data.bool(m,"visible",true)&&visible.contains(Data.id(m,"poiId")))).map(LinkedHashMap::new).map(m->(Map<String,Object>)m).toList();
        }
        try(Connection c=db.open()) {
            return Database.rows(c,SELECT+(admin?"":"WHERE n.is_visible=1 AND l.is_active=1 AND p.is_visible=1 AND ca.is_visible=1 ")+"ORDER BY n.id");
        }
        catch(SQLException e) {
            throw Database.friendly(e);
        }
    }
    public Map<String,Object> get(long id,boolean admin)throws Exception {
        return list(admin).stream().filter(m->Data.id(m,"id")==id).findFirst().orElseThrow(()->new Http.Error(404,"Nội dung không tồn tại hoặc đã ẩn."));
    }
    public synchronized long save(long id,Map<String,Object> input,String cookie)throws Exception {
        long poi=Data.id(input,"poiId"),lang=Data.id(input,"languageId");
        if(settings.demo()) {
            var pois=Data.list(Http.call("GET",settings.poi()+"/api/admin/pois",cookie,null).require().json());
            if(pois.stream().noneMatch(m->Data.id(m,"id")==poi))throw new Http.Error(400,"POI không tồn tại.");
            Map<String,Object> language=languages().stream().filter(m->Data.id(m,"id")==lang).findFirst().orElseThrow(()->new Http.Error(400,"Ngôn ngữ không được hỗ trợ."));
            final long checkId=id;
            if(rows.stream().anyMatch(m->Data.id(m,"id")!=checkId&&Data.id(m,"poiId")==poi&&Data.id(m,"languageId")==lang))throw new Http.Error(409,"POI và ngôn ngữ này đã có nội dung. Hãy chọn Sửa.");
            Map<String,Object> next=new LinkedHashMap<>(input);
            next.put("languageCode",language.get("languageCode"));
            next.put("languageName",language.get("languageName"));
            if(id==0) {
                id=JsonStore.next(rows);
                next.put("id",id);
                rows.add(next);
            }
            else {
                Map<String,Object> old=rows.stream().filter(m->Data.id(m,"id")==checkId).findFirst().orElseThrow(()->new Http.Error(404,"Nội dung không tồn tại."));
                if(Data.id(old,"poiId")!=poi||Data.id(old,"languageId")!=lang)throw new Http.Error(409,"Không đổi POI/ngôn ngữ của bài cũ, để bảo toàn lịch sử audio.");
                next.put("id",id);
                rows.set(rows.indexOf(old),next);
            }
            store.write(rows);
            return id;
        } try(Connection c=db.open()) {
            if(Database.rows(c,"SELECT id FROM languages WHERE id=? AND is_active=1",lang).isEmpty()||Database.rows(c,"SELECT id FROM points_of_interest WHERE id=?",poi).isEmpty())throw new Http.Error(400,"POI/ngôn ngữ không hợp lệ.");
            if(id==0)return Database.insert(c,"INSERT INTO narration_contents(poi_id,language_id,title,content,is_visible) VALUES(?,?,?,?,?)",poi,lang,input.get("title"),input.get("content"),input.get("visible"));
            var old=Database.rows(c,"SELECT poi_id AS poiId,language_id AS languageId FROM narration_contents WHERE id=?",id);
            if(old.isEmpty())throw new Http.Error(404,"Không tìm thấy nội dung.");
            if(Data.id(old.getFirst(),"poiId")!=poi||Data.id(old.getFirst(),"languageId")!=lang)throw new Http.Error(409,"Không thể thay POI/ngôn ngữ của nội dung cũ.");
            Database.update(c,"UPDATE narration_contents SET title=?,content=?,is_visible=? WHERE id=?",input.get("title"),input.get("content"),input.get("visible"),id);
            return id;
        }
        catch(SQLException e) {
            throw Database.friendly(e);
        }
    }
}
