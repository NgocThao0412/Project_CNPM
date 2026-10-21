package com.sgu.client;

import java.util.*;
import java.math.BigDecimal;

/** JSON encoder/decoder nho, Java 21, khong can thu vien ngoai. */
public final class Json {
    private Json() {}
    public static Object parse(String s) { Parser p=new Parser(s); Object v=p.read(); p.ws(); if(p.i!=s.length()) throw new IllegalArgumentException("Dư dữ liệu JSON"); return v; }
    @SuppressWarnings("unchecked")
    public static Map<String,Object> obj(Object o) {
        if (!(o instanceof Map<?,?> m)) throw new IllegalArgumentException("API không trả về JSON object");
        return (Map<String,Object>)m;
    }
    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        if (!(o instanceof List<?> l)) throw new IllegalArgumentException("API không trả về JSON array");
        return (List<Object>)l;
    }
    public static String write(Object o) {
        if(o==null) return "null";
        if(o instanceof String s) return '"'+escape(s)+'"';
        if(o instanceof Number || o instanceof Boolean) return o.toString();
        if(o instanceof Map<?,?> m) { StringJoiner j=new StringJoiner(",","{","}"); m.forEach((k,v)->j.add(write(String.valueOf(k))+":"+write(v))); return j.toString(); }
        if(o instanceof Iterable<?> l) { StringJoiner j=new StringJoiner(",","[","]"); for(Object v:l)j.add(write(v));return j.toString(); }
        throw new IllegalArgumentException("Không thể mã hóa JSON: " + o.getClass());
    }
    private static String escape(String s) {
        StringBuilder b=new StringBuilder();
        for(char c:s.toCharArray()) switch(c) {
            case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\");
            case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r");
            case '\t' -> b.append("\\t"); case '\b' -> b.append("\\b");
            case '\f' -> b.append("\\f"); default -> { if(c<0x20) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
        }
        return b.toString();
    }
    private static final class Parser {
        final String s; int i;
        Parser(String s){this.s=s;}
        void ws(){while(i<s.length() && Character.isWhitespace(s.charAt(i))) i++;}
        char ch(){if(i>=s.length()) throw new IllegalArgumentException("JSON bị cắt ở cuối"); return s.charAt(i);}
        void expect(char c){ws();if(ch()!=c)throw new IllegalArgumentException("JSON sai vị trí "+i+", cần "+c);i++;}
        Object read(){ws();char c=ch(); if(c=='{')return map(); if(c=='[')return list(); if(c=='"')return str();
            if(c=='t'&&s.startsWith("true",i)){i+=4;return true;}
            if(c=='f'&&s.startsWith("false",i)){i+=5;return false;}
            if(c=='n'&&s.startsWith("null",i)){i+=4;return null;}
            if(c=='-'||Character.isDigit(c))return num();
            throw new IllegalArgumentException("JSON sai tại vị trí "+i);
        }
        Object map(){expect('{'); Map<String,Object> m=new LinkedHashMap<>(); ws(); if(ch()=='}'){i++;return m;}
            while(true){ws();String key=str();expect(':');m.put(key,read());ws();char c=ch();i++;if(c=='}')break;if(c!=',')throw new IllegalArgumentException("JSON object thiếu dấu ,");}return m;}
        Object list(){expect('[');List<Object> a=new ArrayList<>();ws();if(ch()==']'){i++;return a;}
            while(true){a.add(read());ws();char c=ch();i++;if(c==']')break;if(c!=',')throw new IllegalArgumentException("JSON array thiếu dấu ,");}return a;}
        String str(){expect('"');StringBuilder b=new StringBuilder();while(true){char c=ch();i++;if(c=='"')return b.toString();if(c=='\\'){
                char e=ch();i++;switch(e){case '"' -> b.append('"');case '\\' -> b.append('\\');case '/' -> b.append('/');case 'n' -> b.append('\n');case 'r' -> b.append('\r');case 't' -> b.append('\t');case 'b' -> b.append('\b');case 'f' -> b.append('\f');case 'u' -> {if(i+4>s.length())throw new IllegalArgumentException("Unicode escape hỏng");b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;}default -> throw new IllegalArgumentException("Escape không hợp lệ");}}
                else {if(c<0x20)throw new IllegalArgumentException("Ký tự điều khiển không hợp lệ");b.append(c);}}}
        Object num(){int start=i;if(ch()=='-')i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;
            if(i<s.length()&&s.charAt(i)=='.'){i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}
            if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}
            return new BigDecimal(s.substring(start,i));}
    }
}
