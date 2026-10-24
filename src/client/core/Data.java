package core;
import java.util.*;
public final class Data {
    private Data() {
    }
    public static String text(Map<String,Object> m,String k) {
        Object v=m.get(k);
        return v==null?"":v.toString();
    }
    public static long id(Map<String,Object> m,String k) {
        try {
            return Long.parseLong(text(m,k));
        }
        catch(Exception e) {
            throw new Http.Error(400,"Invalid "+k);
        }
    }
    public static double number(Map<String,Object> m,String k) {
        try {
            double n=Double.parseDouble(text(m,k));
            if(!Double.isFinite(n))throw new IllegalArgumentException();
            return n;
        }
        catch(Exception e) {
            throw new Http.Error(400,"Invalid "+k);
        }
    }
    public static boolean bool(Map<String,Object> m,String k,boolean fallback) {
        Object v=m.get(k);
        if(v==null)return fallback;
        return Boolean.TRUE.equals(v)||"true".equals(v.toString())||"1".equals(v.toString());
    } @SuppressWarnings("unchecked") public static Map<String,Object> map(Object o) {
        if(!(o instanceof Map))throw new Http.Error(502,"Invalid API response object");
        return (Map<String,Object>)o;
    }
    public static List<Map<String,Object>> list(Object o) {
        if(!(o instanceof List<?> l))throw new Http.Error(502,"Invalid API response list");
        return l.stream().map(Data::map).toList();
    }
    public static String required(Map<String,Object> m,String k,int max) {
        String s=text(m,k).trim();
        if(s.isEmpty()||s.length()>max)throw new Http.Error(400,"Vui lòng nhập "+k+" (tối đa "+max+" ký tự).");
        return s;
    }
    public static Map<String,Object> row(Object... pairs) {
        Map<String,Object> m=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)m.put(pairs[i].toString(),pairs[i+1]);
        return m;
    }
}
