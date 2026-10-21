package com.sgu.client;

import com.sgu.client.Models.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** HTTP Java 21 client: CookieManager giu JSESSIONID backend Spring Boot. */
public final class HttpApi implements ClientApi {
    private final ClientConfig config;
    private final HttpClient client;
    public HttpApi(ClientConfig config){
        this.config=config;
        CookieManager jar=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        this.client=HttpClient.newBuilder().cookieHandler(jar)
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }
    @Override public boolean isDemo(){return false;}
    private URI url(String service,String path){return URI.create(config.endpoint(service)+path);}
    private Object request(String service,String method,String path,Object body) throws Exception {
        HttpRequest.Builder b=HttpRequest.newBuilder(url(service,path))
                .timeout(Duration.ofSeconds(12)).header("Accept","application/json");
        if(body==null) b.method(method,HttpRequest.BodyPublishers.noBody());
        else b.header("Content-Type","application/json; charset=UTF-8")
                .method(method,HttpRequest.BodyPublishers.ofString(Json.write(body),StandardCharsets.UTF_8));
        HttpResponse<String> response;
        try {response=client.send(b.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}
        catch(ConnectException e){throw new IOException("Không kết nối được "+service+" service: "+config.endpoint(service),e);}
        catch(HttpTimeoutException e){throw new IOException("API "+service+" phản hồi quá chậm.",e);}
        if(response.statusCode()<200||response.statusCode()>=300){
            String message="HTTP "+response.statusCode()+" ("+method+" "+path+")";
            try {Object o=Json.parse(response.body());Object m=Json.obj(o).get("message");if(m!=null)message+=" — "+m;}catch(Exception ignored){}
            throw new IOException(message);
        }
        String txt=response.body().trim();return txt.isBlank()?null:Json.parse(txt);
    }
    private void feature(String key,String what){if(!config.enabled(key))throw new UnsupportedOperationException(
            "Backend chưa có API "+what+". Cần hoàn thiện service Java và bật feature."+key+"=true trong config.properties.");}
    @Override public Admin login(String username,String password)throws Exception{
        Map<String,Object> r=Json.obj(request("auth","POST","/api/auth/login",Map.of("username",username,"password",password)));
        return admin(Json.obj(r.get("admin")));
    }
    private static Admin admin(Map<String,Object> r){return new Admin(Models.lng(r.get("id")),Models.string(r.get("username")),Models.string(r.get("fullName")),Models.string(r.get("email")));}
    @Override public Admin me()throws Exception{return admin(Json.obj(request("auth","GET","/api/auth/me",null)));}
    @Override public void logout()throws Exception{request("auth","POST","/api/auth/logout",null);}
    @Override public List<Poi> pois()throws Exception{
        List<Poi> result=new ArrayList<>();
        for(Object o:Json.arr(request("poi","GET","/api/pois",null))) result.add(Models.parsePoi(Json.obj(o)));
        return result;
    }
    @Override public Poi savePoi(Poi p)throws Exception{
        Models.validatePoi(p);feature("poi.write","thêm/sửa POI");
        String method=p.id()==0?"POST":"PUT";
        String path=p.id()==0?"/api/pois":"/api/pois/"+p.id();
        return Models.parsePoi(Json.obj(request("poi",method,path,Models.poiBody(p))));
    }
    @Override public void visiblePoi(long id,boolean visible)throws Exception{
        feature("poi.write","ẩn/hiện POI");
        request("poi","PATCH","/api/pois/"+id+"/visibility",Map.of("isVisible",visible));
    }
    @Override public List<Narration> narrations()throws Exception{
        feature("narration.read","xem nội dung thuyết minh");
        List<Narration> result=new ArrayList<>();
        for(Object o:Json.arr(request("narration","GET","/api/narrations",null)))result.add(Models.parseNarration(Json.obj(o)));
        return result;
    }
    @Override public Narration saveNarration(Narration n)throws Exception{
        Models.validateNarration(n);feature("narration.write","thêm/sửa nội dung thuyết minh");
        String method=n.id()==0?"POST":"PUT";
        String path=n.id()==0?"/api/narrations":"/api/narrations/"+n.id();
        return Models.parseNarration(Json.obj(request("narration",method,path,Models.narrationBody(n))));
    }
    @Override public void visibleNarration(long id,boolean visible)throws Exception{
        feature("narration.write","ẩn/hiện thuyết minh");
        request("narration","PATCH","/api/narrations/"+id+"/visibility",Map.of("isVisible",visible));
    }
    @Override public void uploadAudio(long narrationId,Path audio)throws Exception{
        feature("audio.write","tải audio");
        long size=Files.size(audio);
        if(size>20*1024*1024)throw new IllegalArgumentException("File audio tối đa 20 MB.");
        if(!audio.getFileName().toString().matches("(?i).+\\.(mp3|wav|ogg|m4a)"))throw new IllegalArgumentException("File audio không hợp lệ.");
        String boundary="Client"+System.nanoTime();
        String filename=audio.getFileName().toString().replace("\"", "_").replace("\r", "_").replace("\n","_");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        bytes.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"audio\"; filename=\""+filename+"\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        Files.copy(audio,bytes);bytes.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest req=HttpRequest.newBuilder(url("audio","/api/audio/narrations/"+narrationId))
                .header("Content-Type","multipart/form-data; boundary="+boundary)
                .timeout(Duration.ofSeconds(35)).POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
        HttpResponse<String> res=client.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(res.statusCode()<200||res.statusCode()>=300)throw new IOException("Tải audio thất bại: HTTP "+res.statusCode()+" "+res.body());
    }
    @Override public void listenEvent(long poiId,String lang,boolean simulated)throws Exception{
        feature("visitor.events","ghi nhận lượt nghe");
        request("visitor","POST","/api/listening-logs",Map.of("poiId",poiId,"languageCode",lang,"positionSource",simulated?"SIMULATION":"GPS"));
    }
}
