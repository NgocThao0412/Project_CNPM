import core.*;
import web.DAO.VisitorActivityDAO;
import web.DTO.ClientSessionDTO;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Test Client-only HTTP contract against a strict mock server on a dedicated port. */
public final class VisitorLiveContractTest {
    public static void main(String[] args) throws Exception {
        Settings settings = new Settings();
        int port = Integer.parseInt(System.getenv().getOrDefault("VISITOR_TEST_PORT", "18885"));
        HttpServer mock=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        AtomicInteger calls=new AtomicInteger();
        String token="mock-visitor-session-token-123";
        mock.createContext("/api/visitor/sessions", x -> {
            if(!x.getRequestMethod().equals("POST"))throw new AssertionError("POST expected");
            calls.incrementAndGet();Http.json(x,201,Data.row("token",token));x.close();
        });
        mock.createContext("/api/visitor/heartbeat",x -> {
            if(!x.getRequestMethod().equals("POST") ||
                !token.equals(x.getRequestHeaders().getFirst("X-Visitor-Token")))
                throw new AssertionError("Missing required visitor token header");
            Map<String,Object> json=Http.jsonBody(x);
            if(!(json.get("listening") instanceof Boolean))throw new AssertionError("Missing listening boolean");
            calls.incrementAndGet();x.sendResponseHeaders(204,-1);x.close();
        });
        mock.createContext("/api/visitor/end",x -> {
            if(!token.equals(x.getRequestHeaders().getFirst("X-Visitor-Token")))
                throw new AssertionError("End missing header");
            calls.incrementAndGet();x.sendResponseHeaders(204,-1);x.close();
        });
        mock.createContext("/api/admin/visitor-stats",x -> {
            if(!"SGU_ADMIN_SESSION=mock-cookie".equals(x.getRequestHeaders().getFirst("Cookie")))
                throw new AssertionError("Admin authentication cookie not forwarded");
            calls.incrementAndGet();Http.json(x,200,Data.row("activeVisitors",4,"activeListeners",2));x.close();
        });
        mock.setExecutor(Executors.newVirtualThreadPerTaskExecutor());mock.start();
        try {
            VisitorActivityDAO dao=new VisitorActivityDAO(settings);
            String issued=dao.create();
            if(!issued.equals(token))throw new AssertionError("Token wrong");
            dao.heartbeat(issued,false);dao.heartbeat(issued,true);dao.end(issued);
            ClientSessionDTO admin=new ClientSessionDTO();
            admin.admin=Data.row("username","admin");
            admin.upstreamCookie="SGU_ADMIN_SESSION=mock-cookie";
            Map<String,Object> data=dao.stats(admin);
            if(Data.id(data,"activeVisitors")!=4||Data.id(data,"activeListeners")!=2||calls.get()!=5)
                throw new AssertionError("Forwarded response invalid / missing call");
            System.out.println("PASS: live visitor contract: token issuance, header, heartbeat boolean, end, admin cookie");
        } finally {mock.stop(0);}
    }
}
