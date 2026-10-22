import com.sgu.core.*;
import com.sgu.web.BUS.ClientBUS;
import com.sgu.web.view.Html;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
/** No testing dependency needed. Start all demo servers before running this class. */
public final class ClientTests {
    static int passed;
    static String base,gateway;
    static byte[] mp3;
    static void ok(boolean condition,String name) {
        if(!condition)throw new AssertionError(name);
        passed++;
        System.out.println("PASS "+passed+" - "+name);
    } static void rejects(Runnable test,String name) {
        try {
            test.run();
        }
        catch(RuntimeException e) {
            ok(true,name);
            return;
        }
        throw new AssertionError("Not rejected: "+name);
    } static Http.Reply call(String method,String path,String cookie,Object data)throws Exception {
        return Http.call(method,gateway+path,cookie,data);
    } static String cookie(Http.Reply r,String name) {
        return r.headers().entrySet().stream().filter(e->e.getKey().equalsIgnoreCase("set-cookie")).flatMap(e->e.getValue().stream()).filter(v->v.startsWith(name+"=")).reduce((a,b)->b).orElse("").split(";")[0];
    } static String csrf(Http.Reply r) {
        var m=java.util.regex.Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(new String(r.body(),StandardCharsets.UTF_8));
        if(!m.find())throw new AssertionError("CSRF token missing");
        return m.group(1);
    } static byte[] form(Map<String,String> f) {
        return f.entrySet().stream().map(e->Http.enc(e.getKey())+"="+Http.enc(e.getValue())).reduce((a,b)->a+"&"+b).orElse("").getBytes(StandardCharsets.UTF_8);
    } static Http.Reply postForm(String path,String cookie,Map<String,String> f)throws Exception {
        return Http.request("POST",base+path,cookie,"application/x-www-form-urlencoded",form(f),Map.of());
    } static void unitTests() {
        ok(Json.stringify(Json.parse("{\"x\":[1,true,null,\"SGU\"]}")).equals("{\"x\":[1,true,null,\"SGU\"]}"),"JSON roundtrip");
        ok(Data.text(Json.object("{\"text\":\"\\u0110\\u1ea1i h\\u1ecdc\"}"),"text").equals("Đại học"),"JSON Vietnamese Unicode");
        ok(Json.parse("[1.25,-3e2,0]") instanceof List,"JSON numbers");
        rejects(()->Json.parse("{\"x\":1,\"x\":2}"),"Reject duplicate JSON keys");
        rejects(()->Json.parse("[01]"),"Reject leading-zero number");
        rejects(()->Json.parse("true false"),"Reject JSON trailing data");
        rejects(()->Json.parse("{\"x\":\"a\n\"}"),"Reject control characters");
        rejects(()->Json.stringify(Double.NaN),"Reject NaN output");
        ok(Html.e("<script>\"'&").equals("&lt;script&gt;&quot;&#39;&amp;"),"Escape dynamic HTML");
        ok(Range.parse(null,100).length()==100,"Full byte range");
        ok(Range.parse("bytes=0-1",100).length()==2,"Safari two-byte probe");
        ok(Range.parse("bytes=50-",100).start()==50,"Open-ended range");
        ok(Range.parse("bytes=-12",100).start()==88,"Suffix range");
        ok(Range.parse("bytes=90-200",100).end()==99,"Clamp range end");
        rejects(()->Range.parse("bytes=101-",100),"Reject beyond-file range");
        rejects(()->Range.parse("bytes=9-4",100),"Reject reversed range");
        rejects(()->Range.parse("bytes=0-1,4-5",100),"Reject multipart range safely");
        rejects(()->Range.parse("bytes=-0",100),"Reject empty suffix");
        ok(MP3.inspect(mp3).duration()>10,"Validate actual bundled MP3 frames");
        rejects(()->MP3.inspect("this is not an mp3".getBytes()),"Reject renamed fake MP3");
        ok(Math.abs(ClientBUS.distance(0,0,0,0))<.001,"GPS distance at same point");
        Map<String,Object> p=Data.row("id",1,"latitude",10.75995,"longitude",106.68205,"triggerRadiusM",25,"visible",true);
        ok(ClientBUS.nearest(List.of(p),10.75995,106.68205).isPresent(),"POI inside its own radius");
        ok(ClientBUS.nearest(List.of(p),11,107).isEmpty(),"Do not auto-select a POI outside radius");
        rejects(()->ClientBUS.nearest(List.of(p),91,0),"Reject invalid GPS coordinates");
        p.put("visible",false);
        ok(ClientBUS.nearest(List.of(p),10.75995,106.68205).isEmpty(),"Hidden POI cannot be simulated");
        String bound="TestBoundary";
        byte[] multipart=("--"+bound+"\r\nContent-Disposition: form-data; name=\"id\"\r\n\r\n17\r\n--"+bound+"--\r\n").getBytes(StandardCharsets.ISO_8859_1);
        ok(Multipart.parse("multipart/form-data; boundary="+bound,multipart).get("id").text().equals("17"),"Multipart text field parsing");
    } static void integrationTests()throws Exception {
        ok("demo".equals(Data.map(call("GET","/health","",null).json()).get("mode")),"Tests run only in explicit DEMO mode");
        ok(call("GET","/api/admin/pois","",null).status()==401,"Unauthenticated admin API denied");
        ok(call("POST","/api/auth/login","",Data.row("username","admin","password","wrong")).status()==401,"Wrong password denied");
        Http.Reply login=call("POST","/api/auth/login","",Data.row("username","admin","password","123456"));
        String auth=cookie(login,"SGU_ADMIN_SESSION");
        ok(login.status()==200&&!auth.isEmpty(),"Login returns the exact SGU_ADMIN_SESSION cookie");
        ok(call("GET","/api/auth/me",auth,null).status()==200,"Session propagated to auth/me");
        var poi=Data.row("poiCode","QA-"+System.nanoTime(),"poiName","QA poi","areaId",1,"description","Integration test record","latitude",10.76,"longitude",106.682,"triggerRadiusM",30);
        var added=call("POST","/api/admin/pois",auth,poi);
        ok(added.status()==201,"Create POI via actual gateway");
        long poiId=Data.id(Data.map(added.json()),"id");
        ok(call("POST","/api/admin/pois",auth,poi).status()==409,"Duplicate POI rejected");
        var invalid=new LinkedHashMap<>(poi);
        invalid.put("latitude",200);
        ok(call("POST","/api/admin/pois",auth,invalid).status()==400,"Invalid POI coordinates rejected");
        poi.put("poiName","QA edited");
        ok(call("PUT","/api/admin/pois/"+poiId,auth,poi).status()==200,"Edit POI through REST");
        ok(Data.list(call("GET","/api/admin/pois",auth,null).json()).stream().anyMatch(m->Data.id(m,"id")==poiId&&Data.text(m,"poiName").equals("QA edited")),"Updated POI is read back");
        ok(call("PATCH","/api/admin/pois/"+poiId+"/visibility",auth,Data.row("visible",false)).status()==200,"Hide POI");
        ok(Data.list(call("GET","/api/pois","",null).json()).stream().noneMatch(m->Data.id(m,"id")==poiId),"Hidden POI excluded from visitor API");
        call("PATCH","/api/admin/pois/"+poiId+"/visibility",auth,Data.row("visible",true)).require();
        var story=Data.row("poiId",poiId,"languageId",1,"title","QA narration","content","Narration content <script> is plain text.","visible",true);
        var nr=call("POST","/api/admin/narrations",auth,story);
        ok(nr.status()==201,"Create narration linked to new POI");
        long contentId=Data.id(Data.map(nr.json()),"id");
        ok(call("POST","/api/admin/narrations",auth,story).status()==409,"Duplicate POI-language narration rejected");
        var publicStories=Data.list(call("GET","/api/narrations?poiId="+poiId+"&languageCode=vi","",null).json());
        ok(publicStories.size()==1&&Data.id(publicStories.getFirst(),"id")==contentId,"Read the right narration for POI and language");
        ok(Data.list(call("GET","/api/narrations?poiId="+poiId+"&languageCode=en","",null).json()).isEmpty(),"No false fallback to the wrong language");
        var wrongIdentity=new LinkedHashMap<>(story);
        wrongIdentity.put("languageId",2);
        ok(call("PUT","/api/admin/narrations/"+contentId,auth,wrongIdentity).status()==409,"Cannot reassign existing narration language");
        String upload="/api/admin/audio?contentId="+contentId+"&filename=qa.mp3";
        ok(Http.request("POST",gateway+upload,"","audio/mpeg",mp3,Map.of()).status()==401,"Audio upload requires auth");
        ok(Http.request("POST",gateway+upload,auth,"audio/mpeg","fake".getBytes(),Map.of()).status()==400,"Server validates MP3 rather than extension only");
        Http.Reply ar=Http.request("POST",gateway+upload,auth,"audio/mpeg",mp3,Map.of());
        ok(ar.status()==201,"Upload actual MP3 through gateway to audio-server");
        long audioId=Data.id(Data.map(ar.json()),"id");
        var audio=Data.list(call("GET","/api/audio?contentId="+contentId,"",null).json());
        ok(audio.size()==1&&Data.id(audio.getFirst(),"id")==audioId,"Audio metadata linked to narration ID");
        String media=base+"/media/audio/"+audioId;
        Http.Reply full=Http.request("GET",media,"",null,null,Map.of());
        ok(full.status()==200&&Arrays.equals(full.body(),mp3),"Client streams exact MP3 bytes, not an external player");
        Http.Reply head=Http.request("HEAD",media,"",null,null,Map.of());
        ok(head.status()==200&&Integer.parseInt(head.header("Content-Length"))==mp3.length,"Client HEAD carries correct audio length");
        Http.Reply range=Http.request("GET",media,"",null,null,Map.of("Range","bytes=0-63"));
        ok(range.status()==206&&range.body().length==64&&Arrays.equals(range.body(),Arrays.copyOf(mp3,64)),"Range 206 and exact first 64 bytes across all hops");
        ok(range.header("Content-Range").equals("bytes 0-63/"+mp3.length),"Content-Range preserved by gateway and Client");
        var suffix=Http.request("GET",media,"",null,null,Map.of("Range","bytes=-32"));
        ok(suffix.status()==206&&Arrays.equals(suffix.body(),Arrays.copyOfRange(mp3,mp3.length-32,mp3.length)),"Suffix range passes through all services");
        var bad=Http.request("GET",media,"",null,null,Map.of("Range","bytes="+(mp3.length+1)+"-"));
        ok(bad.status()==416&&bad.header("Content-Range").equals("bytes */"+mp3.length),"416 forwarded correctly");
        ar=Http.request("POST",gateway+upload,auth,"audio/mpeg",mp3,Map.of());
        long replacement=Data.id(Data.map(ar.json()),"id");
        ok(replacement!=audioId&&Data.id(Data.map(ar.json()),"versionNumber")==2,"Replacing audio creates a new version");
        ok(Http.request("GET",media,"",null,null,Map.of()).status()==404,"Old audio is no longer publicly playable");
        call("PATCH","/api/admin/pois/"+poiId+"/visibility",auth,Data.row("visible",false)).require();
        ok(call("GET","/api/audio/"+replacement+"/stream","",null).status()==404,"Hidden POI also hides its audio from visitors");
        ok(call("GET","/api/audio/"+replacement+"/stream?admin=1",auth,null).status()==200,"Authenticated preview of hidden POI audio works");
        Http.Reply lp=Http.call("GET",base+"/login","",null);
        String front=cookie(lp,"SGU_CLIENT"),token=csrf(lp);
        ok(!front.isEmpty()&&!token.isEmpty(),"Login page has independent frontend session and CSRF token");
        ok(postForm("/login",front,Map.of("csrf","wrong","username","admin","password","123456")).status()==403,"CSRF protection on frontend forms");
        Http.Reply fl=postForm("/login",front,Map.of("csrf",token,"username","admin","password","123456"));
        String signedIn=cookie(fl,"SGU_CLIENT");
        ok(fl.status()==303&&!signedIn.equals(front),"Web login rotates the frontend session");
        Http.Reply page=Http.call("GET",base+"/admin",signedIn,null);
        ok(page.status()==200&&new String(page.body(),StandardCharsets.UTF_8).contains("WORKSPACE"),"Admin page loads after web login");
        ok(Http.call("GET",base+"/admin",front,null).status()==303,"Old pre-login session cannot access admin");
        ok(postForm("/logout",signedIn,Map.of("csrf",csrf(page))).status()==303,"Logout form succeeds");
        ok(Http.call("GET",base+"/admin",signedIn,null).status()==303,"Logged-out frontend session denied");
        ok(call("POST","/api/auth/logout",auth,Map.of()).status()==200,"API logout succeeds");
        ok(call("GET","/api/auth/me",auth,null).status()==401,"Revoked backend session denied");
    }
    public static void main(String[] args)throws Exception {
        base=args.length>0?args[0]:"http://localhost:8080";
        gateway=args.length>1?args[1]:"http://127.0.0.1:8080";
        Path home=Path.of(System.getProperty("client.home","."));
        mp3=Files.readAllBytes(home.resolve("audio-server/src/main/resources/audio/demo-vi.mp3"));
        unitTests();
        integrationTests();
        System.out.println("RESULT: "+passed+" checks passed. Live MySQL and physical mobile devices NOT covered by this test.");
    }
}
