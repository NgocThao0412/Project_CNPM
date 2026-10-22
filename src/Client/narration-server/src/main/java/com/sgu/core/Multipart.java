package com.sgu.core;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Bounded multipart parser for one MP3 and textual form fields; filenames are never filesystem paths. */
public final class Multipart {
    public record Part(String filename,String type,byte[] bytes) {
        public String text() {
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }
    public static Map<String,Part> parse(String contentType,byte[] bytes) {
        String boundary=Arrays.stream(contentType.split(";")).map(String::trim).filter(s->s.startsWith("boundary=")).map(s->s.substring(9).replace("\"","")).findFirst().orElseThrow(()->new Http.Error(400,"Missing boundary"));
        if(!boundary.matches("[A-Za-z0-9'()+_,./:=?-]{1,200}"))throw new Http.Error(400,"Invalid boundary");
        String raw=new String(bytes,StandardCharsets.ISO_8859_1);
        String delim="--"+boundary;
        Map<String,Part> out=new LinkedHashMap<>();
        int pos=0;
        while(true) {
            int a=raw.indexOf(delim,pos);
            if(a<0)break;
            a+=delim.length();
            if(raw.startsWith("--",a))break;
            if(!raw.startsWith("\r\n",a))throw new Http.Error(400,"Invalid multipart");
            a+=2;
            int h=raw.indexOf("\r\n\r\n",a);
            if(h<0)throw new Http.Error(400,"Invalid multipart headers");
            String headers=raw.substring(a,h);
            int end=raw.indexOf("\r\n"+delim,h+4);
            if(end<0)throw new Http.Error(400,"Incomplete upload");
            String name=attr(headers,"name");
            String filename=attr(headers,"filename");
            String type=Arrays.stream(headers.split("\r\n")).filter(s->s.toLowerCase(Locale.ROOT).startsWith("content-type:")).map(s->s.substring(13).trim()).findFirst().orElse("text/plain");
            if(name.isEmpty()||out.containsKey(name))throw new Http.Error(400,"Invalid multipart field");
            out.put(name,new Part(filename,type,Arrays.copyOfRange(bytes,h+4,end)));
            pos=end+2;
        }
        return out;
    } private static String attr(String h,String k) {
        var m=java.util.regex.Pattern.compile("(?:^|;\\s*)"+k+"=\"([^\"]*)\"").matcher(h);
        return m.find()?m.group(1):"";
    }
}
