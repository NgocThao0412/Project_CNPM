package core;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
public final class Http {
    private Http() {
    }
    public static final int MAX_BODY=26*1024*1024;
    public static class Error extends RuntimeException {
        public final int status;
        public Error(int s,String m) {
            super(m);
            status=s;
        }
    } @FunctionalInterface public interface Route {
        void handle(HttpExchange x)throws Exception;
    }
    public static HttpServer serve(String bind,int port,Route route)throws IOException {
        HttpServer s=HttpServer.create(new InetSocketAddress(bind,port),100);
        s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        s.createContext("/",x-> {
            try {
                route.handle(x);
            }
            catch(Error e) {
                json(x,e.status,Map.of("message",e.getMessage()));
            }
            catch(IllegalArgumentException e) {
                json(x,400,Map.of("message",e.getMessage()==null?"Invalid request":e.getMessage()));
            }
            catch(Exception e) {
                System.err.println(x.getRequestMethod()+" "+x.getRequestURI().getPath()+" : "+e.getClass().getSimpleName()+" "+e.getMessage());json(x,503,Map.of("message","Dịch vụ chưa sẵn sàng. Kiểm tra server và cấu hình dữ liệu."));
            }
            finally {
                x.close();
            }
        });
        s.start();
        return s;
    }
    public static void headers(HttpExchange x) {
        x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        x.getResponseHeaders().set("Referrer-Policy","same-origin");
        x.getResponseHeaders().set("Cache-Control","no-store");
    }
    public static void bytes(HttpExchange x,int status,String mime,byte[] data)throws IOException {
        headers(x);
        x.getResponseHeaders().set("Content-Type",mime);
        boolean head=x.getRequestMethod().equals("HEAD");
        if(head)x.getResponseHeaders().set("Content-Length",""+data.length);
        x.sendResponseHeaders(status,head?-1:data.length);
        if(!head)x.getResponseBody().write(data);
    }
    public static void json(HttpExchange x,int status,Object value)throws IOException {
        bytes(x,status,"application/json; charset=utf-8",Json.stringify(value).getBytes(StandardCharsets.UTF_8));
    }
    public static void html(HttpExchange x,String html)throws IOException {
        html(x,200,html);
    }
    public static void html(HttpExchange x,int status,String html)throws IOException {
        headers(x);
        x.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; media-src 'self'; frame-src https://www.openstreetmap.org; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
        bytes(x,status,"text/html; charset=utf-8",html.getBytes(StandardCharsets.UTF_8));
    }
    public static void redirect(HttpExchange x,String location)throws IOException {
        if(!location.startsWith("/")||location.startsWith("//")||location.contains("\r")||location.contains("\n"))throw new Error(400,"Invalid redirect");
        headers(x);
        x.getResponseHeaders().set("Location",location);
        x.sendResponseHeaders(303,-1);
    }
    public static byte[] body(HttpExchange x)throws IOException {
        String len=x.getRequestHeaders().getFirst("Content-Length");
        if(len!=null&&Long.parseLong(len)>MAX_BODY)throw new Error(413,"Tập tin tối đa 25 MB.");
        byte[] a=x.getRequestBody().readNBytes(MAX_BODY+1);
        if(a.length>MAX_BODY)throw new Error(413,"Request too large");
        return a;
    }
    public static Map<String,Object> jsonBody(HttpExchange x)throws IOException {
        return Json.object(new String(body(x),StandardCharsets.UTF_8));
    }
    public static String enc(String s) {
        return URLEncoder.encode(s,StandardCharsets.UTF_8);
    }
    public static Map<String,String> params(String s) {
        Map<String,String> m=new LinkedHashMap<>();
        if(s==null||s.isEmpty())return m;
        for(String pair:s.split("&")) {
            String[] v=pair.split("=",2);
            String k=URLDecoder.decode(v[0],StandardCharsets.UTF_8);
            String value=URLDecoder.decode(v.length==2?v[1]:"",StandardCharsets.UTF_8);
            if(m.putIfAbsent(k,value)!=null)throw new Error(400,"Duplicate parameter");
        }
        return m;
    }
    public static Map<String,String> query(HttpExchange x) {
        return params(x.getRequestURI().getRawQuery());
    }
    public static String cookie(HttpExchange x,String key) {
        String h=x.getRequestHeaders().getFirst("Cookie");
        if(h!=null)for(String c:h.split(";")) {
            String[] a=c.trim().split("=",2);
            if(a.length==2&&a[0].equals(key))return a[1];
        }
        return "";
    }
    public static String authCookie(HttpExchange x) {
        String v=cookie(x,"SGU_ADMIN_SESSION");
        return v.matches("[A-Za-z0-9._-]{1,256}")?"SGU_ADMIN_SESSION="+v:"";
    }
    public static String token() {
        return UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");
    }
    public record Reply(int status,Map<String,List<String>> headers,byte[] body) {
        public Object json() {
            try {
                return Json.parse(new String(body,StandardCharsets.UTF_8));
            }
            catch(Exception e) {
                throw new Error(502,"Server trả dữ liệu không đúng JSON.");
            }
        }
        public String header(String name) {
            return headers.entrySet().stream().filter(e->e.getKey().equalsIgnoreCase(name)).flatMap(e->e.getValue().stream()).findFirst().orElse("");
        }
        public Reply require() {
            if(status<200||status>=300) {
                String msg="API HTTP "+status;
                try {
                    msg=Data.text(Data.map(json()),"message");
                }
                catch(Exception ignored) {
                }
                throw new Error(status,msg.isBlank()?"API HTTP "+status:msg);
            }
            return this;
        }
    } private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build();
    public static Reply request(String method,String url,String cookie,String type,byte[] body,Map<String,String> extra)throws IOException,InterruptedException {
        URI uri=URI.create(url);
        if(!Set.of("http","https").contains(uri.getScheme()))throw new Error(400,"Unsupported URL");
        HttpRequest.Builder b=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(25)).header("Accept","*/*");
        if(cookie!=null&&!cookie.isEmpty())b.header("Cookie",cookie);
        if(type!=null&&!type.isBlank())b.header("Content-Type",type);
        extra.forEach(b::header);
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body));
        try {
            HttpResponse<byte[]> r=CLIENT.send(b.build(),HttpResponse.BodyHandlers.ofByteArray());
            return new Reply(r.statusCode(),r.headers().map(),r.body());
        }
        catch(IOException e) {
            throw new Error(503,"Không kết nối được server "+uri.getHost()+":"+uri.getPort()+". Hãy chạy đủ các server.");
        }
    }
    public static Reply call(String method,String url,String cookie,Object data)throws IOException,InterruptedException {
        return request(method,url,cookie,data==null?null:"application/json",data==null?null:Json.stringify(data).getBytes(StandardCharsets.UTF_8),Map.of());
    }
    public static Map<String,Object> requireAdmin(Settings s,String cookie)throws Exception {
        if(cookie==null||cookie.isEmpty())throw new Error(401,"Vui lòng đăng nhập quản trị.");
        return Data.map(call("GET",s.auth()+"/api/auth/me",cookie,null).require().json());
    }
    public static void proxy(HttpExchange x,Reply r)throws IOException {
        for(String h:List.of("Set-Cookie","Content-Range","Accept-Ranges","ETag","Last-Modified","Content-Disposition")) {
            List<String> v=r.headers.entrySet().stream().filter(e->e.getKey().equalsIgnoreCase(h)).map(Map.Entry::getValue).findFirst().orElse(null);
            if(v!=null)x.getResponseHeaders().put(h,v);
        }
        String mime=r.header("Content-Type");
        if(x.getRequestMethod().equals("HEAD")) {
            headers(x);
            if(!mime.isEmpty())x.getResponseHeaders().set("Content-Type",mime);
            String len=r.header("Content-Length");
            if(!len.isEmpty())x.getResponseHeaders().set("Content-Length",len);
            x.sendResponseHeaders(r.status,-1);
        }
        else bytes(x,r.status,mime.isEmpty()?"application/json":mime,r.body);
    }
}
