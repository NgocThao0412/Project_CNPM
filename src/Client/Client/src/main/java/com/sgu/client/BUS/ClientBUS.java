package com.sgu.client.BUS;

import com.sgu.client.DAO.ClientDAO;
import com.sgu.client.DTO.*;
import java.nio.file.Path;
import java.util.List;

/** BUS: nghiep vu Client; DAO chi giao tiep du lieu va HTTP API. */
public final class ClientBUS {
    private final ClientDAO dao;
    public ClientBUS(ClientDAO dao) { this.dao = dao; }
    public boolean isDemo() { return dao.isDemo(); }
    public AdminDTO login(String user, String password) throws Exception {
        if(user == null || user.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập tài khoản và mật khẩu.");
        return dao.login(user.trim(),password);
    }
    public AdminDTO me() throws Exception { return dao.me(); }
    public void logout() throws Exception { dao.logout(); }
    public List<POIDTO> pois() throws Exception { return dao.pois(); }
    public POIDTO savePoi(POIDTO poi) throws Exception {
        ValidationBUS.validatePoi(poi);
        return dao.savePoi(poi);
    }
    public void visiblePoi(long id, boolean visible) throws Exception {
        if(id <= 0) throw new IllegalArgumentException("POI không hợp lệ.");
        dao.visiblePoi(id,visible);
    }
    public List<NarrationDTO> narrations() throws Exception { return dao.narrations(); }
    public NarrationDTO saveNarration(NarrationDTO narration) throws Exception {
        ValidationBUS.validateNarration(narration);
        return dao.saveNarration(narration);
    }
    public void visibleNarration(long id, boolean visible) throws Exception {
        if(id <= 0) throw new IllegalArgumentException("Nội dung không hợp lệ.");
        dao.visibleNarration(id,visible);
    }
    public void uploadAudio(long narrationId, Path file) throws Exception {
        if(narrationId <= 0 || file == null) throw new IllegalArgumentException("File audio hoặc nội dung không hợp lệ.");
        dao.uploadAudio(narrationId,file);
    }
    public void uploadAudio(long narrationId,Path file,double seconds)throws Exception {
        if(narrationId<=0||file==null||!Double.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("File, ID hoặc thời lượng không hợp lệ");
        dao.uploadAudio(narrationId,file,seconds);
    }
    public void listenEvent(long poiId,String lang,boolean simulated) throws Exception { dao.listenEvent(poiId,lang,simulated); }
    public static double haversine(double lat1,double lon1,double lat2,double lon2) {
        double r=6371000;
        double d1=Math.toRadians(lat2-lat1),d2=Math.toRadians(lon2-lon1);
        double a=Math.pow(Math.sin(d1/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(d2/2),2);
        return 2*r*Math.asin(Math.min(1,Math.sqrt(a)));
    }
    public static POIDTO nearestPoi(List<POIDTO> pois,double lat,double lon) {
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)
            throw new IllegalArgumentException("Tọa độ không hợp lệ.");
        POIDTO closest=null;double distance=Double.MAX_VALUE;
        for(POIDTO poi:pois) if(poi.visible()) {
            double d=haversine(lat,lon,poi.latitude(),poi.longitude());
            if(d<distance){closest=poi;distance=d;}
        }
        return closest!=null&&distance<=closest.radiusM()?closest:null;
    }
    public static NarrationDTO findNarration(List<NarrationDTO> all,long poiId,String lang) {
        return all.stream().filter(n->n.visible()&&n.poiId()==poiId&&n.languageCode().equals(lang))
            .findFirst().orElse(null);
    }
}
