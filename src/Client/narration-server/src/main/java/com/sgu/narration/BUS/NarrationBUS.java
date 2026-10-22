package com.sgu.narration.BUS;
import com.sgu.core.*;
import com.sgu.narration.DAO.*;
import java.util.*;
public final class NarrationBUS {
    private final NarrationDAO dao;
    public NarrationBUS(NarrationDAO dao) {
        this.dao=dao;
    }
    public static Map<String,Object> validate(Map<String,Object> m) {
        long poi=Data.id(m,"poiId"),lang=Data.id(m,"languageId");
        if(poi<=0||lang<=0)throw new Http.Error(400,"Vui lòng chọn POI và ngôn ngữ.");
        return Data.row("poiId",poi,"languageId",lang,"title",Data.required(m,"title",200),"content",Data.required(m,"content",100000),"visible",Data.bool(m,"visible",true));
    }
    public long save(long id,Map<String,Object> input,String cookie)throws Exception {
        return dao.save(id,validate(input),cookie);
    }
}
