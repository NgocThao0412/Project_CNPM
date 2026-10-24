package gateway.DAO;
import core.*;
import com.sun.net.httpserver.*;
import java.util.*;
/** Routes only allowlisted service prefixes. Never acts as a user-controlled URL proxy. */
public final class UpstreamDAO {
    private final Settings s;
    public UpstreamDAO(Settings s) {
        this.s=s;
    }
    public void forward(HttpExchange x)throws Exception {
        String p=x.getRequestURI().getPath();
        String base;
        if(p.startsWith("/api/auth/"))base=s.get("auth.url","http://127.0.0.1:8081");
        else if(p.equals("/api/pois")||p.startsWith("/api/admin/pois")||p.equals("/api/campus-areas"))base=s.get("poi.url","http://127.0.0.1:8082");
        else if(p.equals("/api/languages")||p.startsWith("/api/narrations")||p.startsWith("/api/admin/narrations"))base=s.narration();
        else if(p.startsWith("/api/audio")||p.startsWith("/api/admin/audio"))base=s.audio();
        else throw new Http.Error(404,"Endpoint không tồn tại.");
        Map<String,String> headers=new LinkedHashMap<>();
        String range=x.getRequestHeaders().getFirst("Range");
        if(range!=null)headers.put("Range",range);
        byte[] body=Set.of("POST","PUT","PATCH").contains(x.getRequestMethod())?Http.body(x):null;
        Http.Reply reply=Http.request(x.getRequestMethod(),base+x.getRequestURI().toASCIIString(),Http.authCookie(x),x.getRequestHeaders().getFirst("Content-Type"),body,headers);
        if(reply.status()==404&&p.startsWith("/api/admin/pois"))throw new Http.Error(501,"POI service gốc chưa có API sửa/ẩn/danh sách quản trị. Cài phần bổ sung trong integration/poi-service-additions và build lại poi-service.");
        Http.proxy(x,reply);
    }
}
