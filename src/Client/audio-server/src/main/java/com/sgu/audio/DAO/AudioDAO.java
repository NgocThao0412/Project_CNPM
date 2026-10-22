package com.sgu.audio.DAO;
import com.sgu.core.*;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
public final class AudioDAO {
    private final Settings s;
    private final Database db;
    private final JsonStore store;
    private final List<Map<String,Object>> rows;
    private final Path dir;
    private static final String SELECT="SELECT a.id,a.content_id AS contentId,a.file_name AS fileName,a.file_url AS fileUrl,a.duration_seconds AS durationSeconds,a.version_number AS versionNumber,a.is_visible AS visible FROM audio_files a JOIN narration_contents n ON n.id=a.content_id JOIN points_of_interest p ON p.id=n.poi_id JOIN campus_areas ca ON ca.id=p.area_id JOIN languages l ON l.id=n.language_id ";
    public AudioDAO(Settings s)throws Exception {
        this.s=s;
        db=new Database(s);
        dir=s.data("audio");
        if(s.demo()) {
            List<Map<String,Object>> seed=new ArrayList<>();
            for(int i=1;i<=2;i++) {
                String file="demo-"+(i==1?"vi":"en")+".mp3";
                Path dest=dir.resolve(file);
                if(!Files.exists(dest))try(var in=AudioDAO.class.getResourceAsStream("/audio/"+file)) {
                    if(in==null)throw new IllegalStateException("Missing packaged demo audio");
                    Files.copy(in,dest);
                }
                double duration=MP3.inspect(Files.readAllBytes(dest)).duration();
                seed.add(Data.row("id",i,"contentId",i,"fileName",file,"fileUrl",file,"durationSeconds",duration,"versionNumber",1,"visible",true));
            }
            store=new JsonStore(dir.resolve("metadata.json"));
            rows=store.read(seed);
        }
        else {
            store=null;
            rows=null;
        }
    } private static Map<String,Object> publicDTO(Map<String,Object> m) {
        Map<String,Object> out=new LinkedHashMap<>(m);
        out.remove("fileUrl");
        out.put("audioUrl","/api/audio/"+Data.id(m,"id")+"/stream");
        return out;
    }
    public synchronized List<Map<String,Object>> list(long content,boolean admin)throws Exception {
        if(s.demo()) {
            ensureContent(content,admin,"");
            return rows.stream().filter(m->Data.id(m,"contentId")==content&&Data.bool(m,"visible",true)).map(AudioDAO::publicDTO).toList();
        }
        try(Connection c=db.open()) {
            return Database.rows(c,SELECT+"WHERE a.content_id=? AND a.is_visible=1 "+(admin?"":"AND n.is_visible=1 AND p.is_visible=1 AND ca.is_visible=1 AND l.is_active=1 ")+"ORDER BY a.version_number DESC",content).stream().map(AudioDAO::publicDTO).toList();
        }
        catch(SQLException e) {
            throw Database.friendly(e);
        }
    } private void ensureContent(long id,boolean admin,String cookie)throws Exception {
        if(s.demo()&&!admin)Http.call("GET",s.narration()+"/api/narrations/"+id,cookie,null).require();
    } private Map<String,Object> raw(long id,boolean admin)throws Exception {
        if(s.demo()) {
            Map<String,Object> m=rows.stream().filter(x->Data.id(x,"id")==id&&Data.bool(x,"visible",true)).findFirst().orElseThrow(()->new Http.Error(404,"Audio không tồn tại hoặc đã ẩn."));
            ensureContent(Data.id(m,"contentId"),admin,"");
            return m;
        }
        try(Connection c=db.open()) {
            var found=Database.rows(c,SELECT+"WHERE a.id=? AND a.is_visible=1 "+(admin?"":"AND n.is_visible=1 AND p.is_visible=1 AND ca.is_visible=1 AND l.is_active=1"),id);
            if(found.isEmpty())throw new Http.Error(404,"Audio không được công bố.");
            return found.getFirst();
        }
        catch(SQLException e) {
            throw Database.friendly(e);
        }
    }
    public synchronized Path file(long id,boolean admin)throws Exception {
        Map<String,Object> m=raw(id,admin);
        String name=Data.text(m,"fileUrl");
        if(!name.matches("[A-Za-z0-9._-]+\\.mp3"))throw new Http.Error(409,"Audio cũ có đường dẫn chưa hợp lệ. Tải MP3 lên lại trong Quản lý nội dung để tạo phiên bản hợp lệ.");
        Path path=dir.resolve(name).normalize();
        if(!path.startsWith(dir)||!Files.isRegularFile(path))throw new Http.Error(404,"Có thông tin audio nhưng thiếu file MP3 trên audio-server. Hãy tải lên lại.");
        return path;
    }
    public synchronized Map<String,Object> upload(long content,String original,byte[] bytes,String cookie)throws Exception {
        if(content<=0)throw new Http.Error(400,"Missing contentId");
        if(bytes.length>25*1024*1024)throw new Http.Error(413,"MP3 tối đa 25 MB.");
        if(!original.toLowerCase(Locale.ROOT).endsWith(".mp3"))throw new Http.Error(400,"Vui lòng chọn file .mp3.");
        MP3.Info info=MP3.inspect(bytes);
        Http.call("GET",s.narration()+"/api/admin/narrations/"+content,cookie,null).require();
        String filename=original.replace('\\','/');
        filename=filename.substring(filename.lastIndexOf('/')+1);
        if(filename.length()>240)filename=filename.substring(filename.length()-240);
        String storage=UUID.randomUUID()+".mp3";
        Path temp=Files.createTempFile(dir,"upload-",".tmp"),target=dir.resolve(storage);
        Files.write(temp,bytes);
        Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
        try {
            if(s.demo()) {
                int version=rows.stream().filter(m->Data.id(m,"contentId")==content).mapToInt(m->(int)Data.id(m,"versionNumber")).max().orElse(0)+1;
                long id=JsonStore.next(rows);
                List<Map<String,Object>> next=new ArrayList<>();
                for(var old:rows) {
                    Map<String,Object> copy=new LinkedHashMap<>(old);
                    if(Data.id(copy,"contentId")==content)copy.put("visible",false);
                    next.add(copy);
                }
                Map<String,Object> value=Data.row("id",id,"contentId",content,"fileName",filename,"fileUrl",storage,"durationSeconds",info.duration(),"versionNumber",version,"visible",true);
                next.add(value);
                store.write(next);
                rows.clear();
                rows.addAll(next);
                return publicDTO(value);
            } try(Connection c=db.open()) {
                c.setAutoCommit(false);
                try {
                    if(Database.rows(c,"SELECT id FROM narration_contents WHERE id=? FOR UPDATE",content).isEmpty())throw new Http.Error(404,"Nội dung không còn tồn tại.");
                    var versions=Database.rows(c,"SELECT COALESCE(MAX(version_number),0) AS version FROM audio_files WHERE content_id=?",content);
                    long version=Data.id(versions.getFirst(),"version")+1;
                    Database.update(c,"UPDATE audio_files SET is_visible=0 WHERE content_id=? AND is_visible=1",content);
                    long id=Database.insert(c,"INSERT INTO audio_files(content_id,file_name,file_url,mime_type,file_size_bytes,duration_seconds,version_number,is_visible) VALUES(?,?,?,'audio/mpeg',?,?,?,1)",content,filename,storage,bytes.length,info.duration(),version);
                    c.commit();
                    return Data.row("id",id,"contentId",content,"fileName",filename,"audioUrl","/api/audio/"+id+"/stream","durationSeconds",info.duration(),"versionNumber",version,"visible",true);
                }
                catch(Exception e) {
                    c.rollback();
                    throw e;
                }
                finally {
                    c.setAutoCommit(true);
                }
            }
        }
        catch(Exception e) {
            Files.deleteIfExists(target);
            if(e instanceof SQLException sq)throw Database.friendly(sq);
            throw e;
        }
        finally {
            Files.deleteIfExists(temp);
        }
    }
}
