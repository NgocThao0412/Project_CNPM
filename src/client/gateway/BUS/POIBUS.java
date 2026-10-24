package gateway.BUS;
import java.util.*;
import core.*;
public final class POIBUS {
    public static Map<String,Object> validate(Map<String,Object> m) {
        String code=Data.required(m,"poiCode",50),name=Data.required(m,"poiName",200);
        long area=Data.id(m,"areaId");
        double lat=Data.number(m,"latitude"),lon=Data.number(m,"longitude"),radius=Data.number(m,"triggerRadiusM");
        if(area<=0||lat < -90||lat>90||lon< -180||lon>180||radius<=0||radius>999999.99)throw new Http.Error(400,"Kiểm tra khu vực, tọa độ và bán kính GPS.");
        if(Data.text(m,"description").length()>10000)throw new Http.Error(400,"Mô tả quá dài.");
        return Data.row("poiCode",code,"poiName",name,"areaId",area,"description",Data.text(m,"description"),"latitude",lat,"longitude",lon,"triggerRadiusM",radius);
    }
}
