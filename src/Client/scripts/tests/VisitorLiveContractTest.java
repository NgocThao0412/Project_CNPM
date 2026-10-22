import com.sgu.core.Data;
import com.sgu.core.Settings;
import com.sgu.web.DAO.VisitorActivityDAO;
import com.sgu.web.DTO.ClientSessionDTO;
import java.util.Map;
import java.util.UUID;

/** Optional contract check against a mock visitor-service, not shipped as backend code. */
public final class VisitorLiveContractTest {
    public static void main(String[] args) throws Exception {
        Settings settings=new Settings();
        if(settings.demo())throw new AssertionError("APP_MODE=live required");
        VisitorActivityDAO dao=new VisitorActivityDAO(settings);
        dao.activity(Data.row("sessionId",UUID.randomUUID().toString(),"event","play",
                "page","/place","poiId",1,"audioId",4,"listening",true));
        ClientSessionDTO admin=new ClientSessionDTO();
        admin.upstreamCookie="SGU_ADMIN_SESSION=mock-token";
        Map<String,Object> result=dao.online(admin);
        if(Data.id(result,"online")!=3 || Data.id(result,"listening")!=1)
            throw new AssertionError("Wrong live counts from visitor-service");
        System.out.println("LIVE contract proxy test PASS");
    }
}
