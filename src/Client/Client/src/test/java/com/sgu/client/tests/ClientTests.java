package com.sgu.client.tests;

import com.sgu.client.DTO.*;
import com.sgu.client.DAO.*;
import com.sgu.client.BUS.*;
import com.sgu.client.controller.ClientController;
import com.sgu.client.config.ClientConfig;
import com.sgu.client.util.Json;
import com.sgu.client.client.MapPanel;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Chay kiem thu Java thuan bang: java -ea -cp build/classes com.sgu.client.tests.ClientTests */
public final class ClientTests {
    private static int passed=0;
    private static void check(boolean c,String name){if(!c)throw new AssertionError(name);passed++;System.out.println("PASS "+name);}
    private static void expectFailure(Runnable r,String name){boolean fail=false;try{r.run();}catch(Exception e){fail=true;}check(fail,name);}
    public static void main(String[] args)throws Exception{
        Object x=Json.parse("{\"message\":\"Xin chào \\u0110ại học\",\"list\":[1,true,null,\"a\\n\\\"b\"]}");
        check(Json.obj(x).get("message").equals("Xin chào Đại học"),"JSON UTF-8 + unicode escaped");
        check(Json.parse(Json.write(x)).equals(x),"JSON roundtrip");
        DemoDAO demo=new DemoDAO();
        expectFailure(()->demo.login("admin","wrong"),"Demo không nhận mật khẩu sai");
        check(demo.login("admin","123456")!=null,"Demo đăng nhập 123456");
        POIDTO initial=demo.pois().get(0);
        check(demo.pois().size()==4,"Demo có 4 POI minh họa");
        POIDTO added=demo.savePoi(new POIDTO(0,"SGU-TEST","Khu học tập demo",0,"Test","Chỉ demo",10.759,106.683,30,"",true));
        check(added.id()>0 && demo.pois().size()==5,"Demo thêm POI");
        demo.visiblePoi(added.id(),false);
        check(demo.pois().stream().anyMatch(p->p.id()==added.id()&&!p.visible()),"Demo ẩn POI");
        NarrationDTO n=demo.saveNarration(new NarrationDTO(0,added.id(),"en","Test narration","Hello","",true));
        check(n.id()>0 && demo.narrations().size()==4,"Demo thêm narration");
        demo.visibleNarration(n.id(),false);
        check(demo.narrations().stream().anyMatch(y->y.id()==n.id()&&!y.visible()),"Demo ẩn narration");
        expectFailure(()->demo.savePoi(new POIDTO(0,"x","x",0,"","",100,0,50,"",true)),"Từ chối vĩ độ sai");
        demo.logout();
        check(demo.me()==null,"Demo đăng xuất");
        check(Math.abs(ClientBUS.haversine(10,106,10,106))<.00001,"GPS trùng tọa độ = 0m");
        Point2D p=MapPanel.project(10.7601,106.6815,17);
        check(p.getX()>0&&p.getY()>0,"Bản đồ chuyển tọa độ Mercator");
        POIDTO parsed=DtoMapper.parsePoi(Json.obj(Json.parse("{\"id\":3,\"poiCode\":\"SGU-003\",\"poiName\":\"Library\",\"areaId\":2,\"latitude\":10.76,\"longitude\":106.68,\"triggerRadiusM\":50,\"imageUrl\":\"\"}")));
        check(parsed.code().equals("SGU-003")&&parsed.visible(),"Đọc POIDTO Spring Boot");
        check(DtoMapper.poiBody(parsed).get("poiCode").equals("SGU-003"),"Ghi POI theo camelCase của Java backend");
        check(initial != null,"Demo POI load");

        AtomicReference<String> cookieSeen=new AtomicReference<>("");
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/auth/login",e->{
            byte[] req=e.getRequestBody().readAllBytes();Object payload=Json.parse(new String(req,StandardCharsets.UTF_8));
            check(Json.obj(payload).get("username").equals("sguadmin"),"Gửi đúng username cho AuthController");
            e.getResponseHeaders().add("Set-Cookie","JSESSIONID=abc123; Path=/; HttpOnly");
            reply(e,200,"{\"message\":\"OK\",\"admin\":{\"id\":1,\"username\":\"sguadmin\",\"fullName\":\"AdminDTO SGU\",\"email\":\"admin@sgu.edu.vn\"}}");});
        server.createContext("/api/auth/me",e->{cookieSeen.set(e.getRequestHeaders().getFirst("Cookie"));reply(e,200,"{\"id\":1,\"username\":\"sguadmin\",\"fullName\":\"AdminDTO SGU\",\"email\":\"admin@sgu.edu.vn\"}");});
        server.createContext("/api/auth/logout",e->{reply(e,200,"{\"message\":\"Đăng xuất thành công\"}");});
        server.createContext("/api/pois",e->{reply(e,200,"[{\"id\":4,\"poiCode\":\"SGU-004\",\"poiName\":\"Thư viện\",\"areaId\":2,\"areaName\":\"Khu A\",\"description\":\"Thông tin\",\"latitude\":10.76,\"longitude\":106.68,\"triggerRadiusM\":50,\"imageUrl\":null}]");});
        server.start();
        try{
            int port=server.getAddress().getPort();
            Path cfg=Files.createTempFile("client-test",".properties");
            Files.writeString(cfg,"service.gateway=http://127.0.0.1:"+port+"\nfeature.poi.write=false\nfeature.narration.read=false\n");
            HttpDAO api=new HttpDAO(new ClientConfig(cfg));
            check(api.login("sguadmin","123456").fullName().equals("AdminDTO SGU"),"API login Spring Session");
            check(api.me().username().equals("sguadmin"),"API me nhận admin");
            check(cookieSeen.get()!=null&&cookieSeen.get().contains("JSESSIONID=abc123"),"JSESSIONID được giữ trong Java CookieManager");
            check(api.pois().size()==1&&api.pois().get(0).name().equals("Thư viện"),"GET /api/pois trả POIDTO");
            try{api.savePoi(added);throw new AssertionError("Cần bị chặn");}
            catch(UnsupportedOperationException expected){passed++;System.out.println("PASS Không ghi giả khi POI API chưa có");}
            try{api.narrations();throw new AssertionError("Cần bị chặn");}
            catch(UnsupportedOperationException expected){passed++;System.out.println("PASS Không giả lập narration ở chế độ API thật");}
            api.logout();passed++;System.out.println("PASS POST logout qua Java API");
            Files.deleteIfExists(cfg);
        }finally{server.stop(0);}
        // Controller -> BUS -> DAO: round-trip local demo and validation in BUS.
        ClientController ctrl = new ClientController(new ClientConfig(Path.of("config.properties")));
        check(ctrl.isDemo() && ctrl.pois().size()==4,"Controller kết nối BUS và DemoDAO");
        try { ctrl.savePoi(new POIDTO(0,"","",0,"","",0,0,-1,"",true));
            throw new AssertionError("BUS phải chặn dữ liệu sai");
        } catch(IllegalArgumentException expected){passed++;System.out.println("PASS BUS validate POI");}
        check(ClientBUS.nearestPoi(ctrl.pois(),10.76012,106.68162)!=null,"BUS tìm POI theo GPS");
        ctrl.setDemo(false);
        check(!ctrl.isDemo(),"Controller đổi DAO sang HTTP Microservices");
        System.out.println("TỔNG SỐ CHECK ĐẠT: "+passed);
    }
    private static void reply(com.sun.net.httpserver.HttpExchange e,int code,String body)throws java.io.IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");e.sendResponseHeaders(code,data.length);
        try(var os=e.getResponseBody()){os.write(data);}
    }
}
