package com.sgu.client;

import com.sgu.client.Models.*;
import java.nio.file.*;
import java.util.*;

/** Dữ liệu GIẢ LẬP riêng để thử giao diện; không ghi vào MySQL. */
public final class DemoApi implements ClientApi {
    private final List<Poi> pois=new ArrayList<>();
    private final List<Narration> narrations=new ArrayList<>();
    private Admin current;
    public DemoApi(){
        pois.add(new Poi(1,"SGU-001","Cổng chính",0,"Khu trung tâm","Điểm bắt đầu hành trình tham quan (dữ liệu minh họa).",10.76012,106.68162,45,"",true));
        pois.add(new Poi(2,"SGU-002","Thư viện",0,"Khu học tập","Không gian tự học và đọc sách (dữ liệu minh họa).",10.75980,106.68218,45,"",true));
        pois.add(new Poi(3,"SGU-003","Giảng đường",0,"Khu học tập","Khu giảng dạy (dữ liệu minh họa).",10.75949,106.68278,40,"",true));
        pois.add(new Poi(4,"SGU-004","Hội trường",0,"Khu sinh hoạt","Nơi sinh hoạt chung (dữ liệu minh họa).",10.76048,106.68276,50,"",false));
        narrations.add(new Narration(1,1,"vi","Chào mừng đến SGU","Chào mừng bạn đến với Trường Đại học Sài Gòn.","",true));
        narrations.add(new Narration(2,1,"en","Welcome to SGU","Welcome to Saigon University.","",true));
        narrations.add(new Narration(3,2,"vi","Giới thiệu thư viện","Thư viện là nơi đọc và tự học.","",true));
    }
    @Override public boolean isDemo(){return true;}
    @Override public synchronized Admin login(String username,String password){
        if(!(username.equalsIgnoreCase("admin@sgu.edu.vn") || username.equalsIgnoreCase("admin")) || !password.equals("123456"))
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu demo không đúng.");
        return current=new Admin(1,"admin","Quản trị viên Demo","admin@sgu.edu.vn");
    }
    @Override public synchronized Admin me(){return current;}
    @Override public synchronized void logout(){current=null;}
    @Override public synchronized List<Poi> pois(){return List.copyOf(pois);}
    @Override public synchronized Poi savePoi(Poi p){check();Models.validatePoi(p);
        for(Poi other:pois) if(other.id()!=p.id() && other.code().equalsIgnoreCase(p.code()))throw new IllegalArgumentException("Mã POI trùng.");
        long id=p.id()==0?pois.stream().mapToLong(Poi::id).max().orElse(0)+1:p.id();
        Poi result=new Poi(id,p.code(),p.name(),p.areaId(),p.area(),p.description(),p.latitude(),p.longitude(),p.radiusM(),p.imageUrl(),p.visible());
        if(p.id()!=0&&!pois.removeIf(x->x.id()==id))throw new IllegalArgumentException("POI không tồn tại.");
        pois.add(result);return result;
    }
    @Override public synchronized void visiblePoi(long id,boolean shown){check();
        Poi p=pois.stream().filter(x->x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("POI không tồn tại."));
        pois.remove(p);pois.add(new Poi(id,p.code(),p.name(),p.areaId(),p.area(),p.description(),p.latitude(),p.longitude(),p.radiusM(),p.imageUrl(),shown));
    }
    @Override public synchronized List<Narration> narrations(){return List.copyOf(narrations);}
    @Override public synchronized Narration saveNarration(Narration n){check();Models.validateNarration(n);
        if(pois.stream().noneMatch(p->p.id()==n.poiId()))throw new IllegalArgumentException("Không tìm thấy POI.");
        if(narrations.stream().anyMatch(x->x.id()!=n.id()&&x.poiId()==n.poiId()&&x.languageCode().equals(n.languageCode())))throw new IllegalArgumentException("POI đã có nội dung ngôn ngữ này.");
        long id=n.id()==0?narrations.stream().mapToLong(Narration::id).max().orElse(0)+1:n.id();
        if(n.id()!=0&&!narrations.removeIf(x->x.id()==id))throw new IllegalArgumentException("Không tìm thấy nội dung.");
        Narration result=new Narration(id,n.poiId(),n.languageCode(),n.title(),n.content(),n.audioUrl(),n.visible());narrations.add(result);return result;
    }
    @Override public synchronized void visibleNarration(long id,boolean shown){check();
        Narration n=narrations.stream().filter(x->x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Nội dung không tồn tại."));
        narrations.remove(n);narrations.add(new Narration(n.id(),n.poiId(),n.languageCode(),n.title(),n.content(),n.audioUrl(),shown));
    }
    @Override public synchronized void uploadAudio(long id,Path path){check();
        Narration n=narrations.stream().filter(x->x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Nội dung không tồn tại."));
        String name=path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if(!(name.endsWith(".mp3")||name.endsWith(".wav")||name.endsWith(".m4a")||name.endsWith(".ogg")))
            throw new IllegalArgumentException("Chỉ hỗ trợ file âm thanh MP3/WAV/M4A/OGG.");
        if(!java.nio.file.Files.isRegularFile(path)) throw new IllegalArgumentException("File audio không tồn tại.");
        narrations.remove(n);narrations.add(new Narration(n.id(),n.poiId(),n.languageCode(),n.title(),n.content(),path.toUri().toString(),n.visible()));
    }
    @Override public void listenEvent(long poiId,String lang,boolean simulated){} // demo only
    private void check(){if(current==null)throw new IllegalStateException("Bạn chưa đăng nhập.");}
}
