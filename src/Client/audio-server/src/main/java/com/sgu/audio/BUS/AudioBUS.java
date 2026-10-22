package com.sgu.audio.BUS;
import com.sgu.audio.DAO.*;
import com.sgu.core.*;
import com.sun.net.httpserver.*;
import java.nio.file.*;
import java.io.*;
public final class AudioBUS {
    private final AudioDAO dao;
    public AudioBUS(AudioDAO dao) {
        this.dao=dao;
    }
    /** Always supports HEAD/Range. Streams only the selected segment, never an external player. */
    public void stream(HttpExchange x,long id,boolean admin)throws Exception {
        Path path=dao.file(id,admin);
        long total=Files.size(path);
        Range range;
        try {
            range=Range.parse(x.getRequestHeaders().getFirst("Range"),total);
        }
        catch(Http.Error e) {
            x.getResponseHeaders().set("Content-Range","bytes */"+total);
            x.getResponseHeaders().set("Accept-Ranges","bytes");
            x.sendResponseHeaders(416,-1);
            return;
        } Http.headers(x);
        var h=x.getResponseHeaders();
        h.set("Content-Type","audio/mpeg");
        h.set("Accept-Ranges","bytes");
        h.set("Content-Length",""+range.length());
        if(range.partial())h.set("Content-Range","bytes "+range.start()+"-"+range.end()+"/"+total);
        int status=range.partial()?206:200;
        if(x.getRequestMethod().equals("HEAD")) {
            x.sendResponseHeaders(status,-1);
            return;
        }
        x.sendResponseHeaders(status,range.length());
        try(var in=Files.newInputStream(path)) {
            in.skipNBytes(range.start());
            byte[] b=new byte[64*1024];
            long left=range.length();
            while(left>0) {
                int count=in.read(b,0,(int)Math.min(left,b.length));
                if(count<0)break;
                x.getResponseBody().write(b,0,count);
                left-=count;
            }
        }
    }
}
