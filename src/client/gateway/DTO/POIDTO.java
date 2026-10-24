package gateway.DTO;
import java.util.*;
import core.*;
/** The keys match the actual POIDTO/CreatePOIRequestDTO in the supplied poi-service. */
public record POIDTO(long id,String poiCode,String poiName,long areaId,String areaName,String description,double latitude,double longitude,double triggerRadiusM,String imageUrl,boolean visible) {
    public Map<String,Object> map() {
        return Data.row("id",id,"poiCode",poiCode,"poiName",poiName,"areaId",areaId,"areaName",areaName,"description",description,"latitude",latitude,"longitude",longitude,"triggerRadiusM",triggerRadiusM,"imageUrl",imageUrl,"visible",visible);
    }
}
