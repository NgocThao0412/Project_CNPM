package com.sgu.web.BUS;
import java.util.*;
import com.sgu.core.*;
public final class ClientBUS {
    public static Map<String,Object> poiForm(Map<String,String> f) {
        String code=required(f,"poiCode",50),name=required(f,"poiName",200);
        long area=positive(f.get("areaId"));
        double lat=number(f.get("latitude")),lon=number(f.get("longitude")),radius=number(f.get("triggerRadiusM"));
        if(lat< -90||lat>90||lon< -180||lon>180||radius<=0||radius>999999.99)throw new Http.Error(400,"Tọa độ hoặc bán kính GPS không hợp lệ.");
        if(new java.math.BigDecimal(f.get("latitude")).scale()>7||new java.math.BigDecimal(f.get("longitude")).scale()>7)throw new Http.Error(400,"Tọa độ chỉ có tối đa 7 chữ số thập phân.");
        return Data.row("poiCode",code,"poiName",name,"areaId",area,"description",f.getOrDefault("description",""),"latitude",lat,"longitude",lon,"triggerRadiusM",radius);
    }
    public static Map<String,Object> contentForm(Map<String,String> f) {
        return Data.row("poiId",positive(f.get("poiId")),"languageId",positive(f.get("languageId")),"title",required(f,"title",200),"content",required(f,"content",100000),"visible","true".equals(f.getOrDefault("visible","false")));
    } private static String required(Map<String,String> f,String k,int max) {
        String s=f.getOrDefault(k,"").trim();
        if(s.isEmpty()||s.length()>max)throw new Http.Error(400,"Trường "+k+" bắt buộc, tối đa "+max+" ký tự.");
        return s;
    }
    public static long positive(String s) {
        try {
            long n=Long.parseLong(s);
            if(n<=0)throw new NumberFormatException();
            return n;
        }
        catch(Exception e) {
            throw new Http.Error(400,"Vui lòng chọn dữ liệu hợp lệ.");
        }
    }
    public static double number(String s) {
        try {
            double n=Double.parseDouble(s);
            if(!Double.isFinite(n))throw new NumberFormatException();
            return n;
        }
        catch(Exception e) {
            throw new Http.Error(400,"Vui lòng nhập số hợp lệ.");
        }
    }
    public static double distance(double a,double b,double c,double d) {
        double r=6371000,p1=Math.toRadians(a),p2=Math.toRadians(c),da=p2-p1,db=Math.toRadians(d-b),h=Math.sin(da/2)*Math.sin(da/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(db/2)*Math.sin(db/2);
        return 2*r*Math.atan2(Math.sqrt(Math.max(0,h)),Math.sqrt(Math.max(0,1-h)));
    }
    public static Optional<Map<String,Object>> nearest(List<Map<String,Object>> rows,double lat,double lon) {
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat< -90||lat>90||lon< -180||lon>180)throw new Http.Error(400,"Tọa độ không hợp lệ.");
        return rows.stream().filter(m->Data.bool(m,"visible",true)).filter(m->distance(lat,lon,Data.number(m,"latitude"),Data.number(m,"longitude"))<=Data.number(m,"triggerRadiusM")).min(Comparator.comparingDouble(m->distance(lat,lon,Data.number(m,"latitude"),Data.number(m,"longitude"))));
    }
    public static Map<String,Object> byId(List<Map<String,Object>> list,long id) {
        return list.stream().filter(m->Data.id(m,"id")==id).findFirst().orElseThrow(()->new Http.Error(404,"Không tìm thấy dữ liệu."));
    }
}
