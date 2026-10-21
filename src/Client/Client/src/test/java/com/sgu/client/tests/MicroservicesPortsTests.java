package com.sgu.client.tests;

import com.sgu.client.DTO.*;
import com.sgu.client.DAO.HttpDAO;
import com.sgu.client.config.ClientConfig;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Integration contract smoke tests with four independent mock servers and ports. */
public class MicroservicesPortsTests {
    static int passed=0;
    static void assertOk(boolean condition,String desc) {
        if(!condition)throw new AssertionError(desc);
        System.out.println("PASS "+desc);passed++;
    }
    static void reply(HttpExchange x,int status,String body)throws IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type","application/json");
        x.sendResponseHeaders(status,data.length);
        try(var out=x.getResponseBody()){out.write(data);}
    }
    static boolean hasCookie(HttpExchange x){return Objects.toString(x.getRequestHeaders().getFirst("Cookie"),"").contains("JSESSIONID=mock42");}
    public static void main(String[] args)throws Exception {
        HttpServer auth=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        HttpServer poi=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        HttpServer narration=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        HttpServer audio=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicBoolean poiCookie=new AtomicBoolean(false),narrationCookie=new AtomicBoolean(false),audioCookie=new AtomicBoolean(false);
        auth.createContext("/api/auth/login",x->{
            String body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            assertOk(body.contains("\"username\":\"admin\""),"Login goes to separated auth-server");
            x.getResponseHeaders().add("Set-Cookie","JSESSIONID=mock42; Path=/; HttpOnly");
            reply(x,200,"{\"admin\":{\"id\":1,\"username\":\"admin\",\"fullName\":\"Admin\",\"email\":\"admin@example.com\"}}");
        });
        auth.createContext("/api/auth/logout",x->reply(x,200,"{}"));
        poi.createContext("/api/pois/admin",x->{poiCookie.set(hasCookie(x));reply(x,200,"[{\"id\":1,\"poiCode\":\"SGU-001\",\"poiName\":\"Gate\",\"areaId\":1,\"areaName\":\"Campus\",\"latitude\":10.76,\"longitude\":106.68,\"triggerRadiusM\":50,\"isVisible\":false}]");});
        poi.createContext("/api/pois",x->{
            String path=x.getRequestURI().getPath();
            if(path.matches("/api/pois/\\d+/visibility")){
               assertOk(x.getRequestMethod().equals("PATCH")&&hasCookie(x),"PATCH POI on poi-server with session");reply(x,200,"{\"id\":1,\"isVisible\":true}");
            } else if(path.equals("/api/pois")&&x.getRequestMethod().equals("POST")){
               assertOk(hasCookie(x),"POST POI authenticated on poi-server");
               reply(x,200,"{\"id\":2,\"poiCode\":\"NEW\",\"poiName\":\"New\",\"areaId\":1,\"latitude\":10.76,\"longitude\":106.68,\"triggerRadiusM\":50,\"isVisible\":true}");
            } else reply(x,200,"[]");
        });
        narration.createContext("/api/narrations/admin",x->{narrationCookie.set(hasCookie(x));reply(x,200,"[{\"id\":9,\"poiId\":1,\"languageCode\":\"vi\",\"title\":\"Welcome\",\"content\":\"Hello\",\"audioUrl\":\"\",\"isVisible\":false}]");});
        narration.createContext("/api/narrations",x->{
            if(x.getRequestMethod().equals("POST")){
                assertOk(hasCookie(x),"POST narration authenticated on narration-server");
                reply(x,200,"{\"id\":10,\"poiId\":1,\"languageCode\":\"en\",\"title\":\"Hello\",\"content\":\"Good day\",\"audioUrl\":\"\",\"isVisible\":true}");
            }else reply(x,200,"[]");
        });
        audio.createContext("/api/audio/narrations/9",x->{
            audioCookie.set(hasCookie(x));
            assertOk(x.getRequestURI().getQuery().contains("durationSeconds=48.5"),"Audio upload passes actual durationSeconds");
            assertOk(x.getRequestHeaders().getFirst("Content-Type").startsWith("multipart/form-data"),"Audio is multipart upload");
            x.getRequestBody().readAllBytes();reply(x,200,"{\"id\":1}");
        });
        auth.start();poi.start();narration.start();audio.start();
        Path props=Files.createTempFile("services-ports-",".properties"), mp3=Files.createTempFile("mock-audio-",".mp3");
        try {
            Files.writeString(props, "service.auth=http://127.0.0.1:"+auth.getAddress().getPort()+"\n"+
              "service.poi=http://127.0.0.1:"+poi.getAddress().getPort()+"\n"+
              "service.narration=http://127.0.0.1:"+narration.getAddress().getPort()+"\n"+
              "service.audio=http://127.0.0.1:"+audio.getAddress().getPort()+"\n"+
              "feature.poi.write=true\nfeature.narration.read=true\nfeature.narration.write=true\nfeature.audio.write=true\n");
            Files.write(mp3,new byte[]{'I','D','3',3,0,0,0,0});
            HttpDAO client=new HttpDAO(new ClientConfig(props));
            client.login("admin","123456");
            assertOk(client.pois().size()==1&&!client.pois().get(0).visible(),"Admin POI endpoint includes hidden POIs");
            assertOk(poiCookie.get(),"Auth JSESSIONID is forwarded by Client to POI port");
            assertOk(client.narrations().size()==1&&!client.narrations().get(0).visible(),"Admin narration endpoint includes hidden content");
            assertOk(narrationCookie.get(),"Auth JSESSIONID is forwarded to narration port");
            POIDTO created=client.savePoi(new POIDTO(0,"NEW","New",1,"Campus","desc",10.76,106.68,50,"",true));
            assertOk(created.id()==2,"POI create response mapped");
            client.visiblePoi(1,true);
            NarrationDTO n=client.saveNarration(new NarrationDTO(0,1,"en","Hello","Good day","",true));
            assertOk(n.id()==10,"Narration create response mapped");
            client.uploadAudio(9,mp3,48.5);
            assertOk(audioCookie.get(),"Auth JSESSIONID is forwarded to audio port");
            client.logout();
        }finally{
            Files.deleteIfExists(props);Files.deleteIfExists(mp3);
            auth.stop(0);poi.stop(0);narration.stop(0);audio.stop(0);
        }
        System.out.println("TỔNG CHECK MICROservice ĐẠT: "+passed);
    }
}
