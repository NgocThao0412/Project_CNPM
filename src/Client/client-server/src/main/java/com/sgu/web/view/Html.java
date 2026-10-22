package com.sgu.web.view;
import java.util.*;
import com.sgu.core.*;
/** Java presentation helpers. All dynamic text and attributes are escaped, never raw concatenated. */
public final class Html {
    private Html() {
    }
    public static String e(Object v) {
        return v==null?"":v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
    public static String link(String url,String label,String cls) {
        return "<a class=\""+e(cls)+"\" href=\""+e(url)+"\">"+label+"</a>";
    }
    public static String text(String tag,String value,String cls) {
        return "<"+tag+" class=\""+e(cls)+"\">"+e(value)+"</"+tag+">";
    }
    public static String hidden(String key,Object value) {
        return "<input type=\"hidden\" name=\""+e(key)+"\" value=\""+e(value)+"\">";
    }
    public static String input(String label,String name,Object value,String type,String extra) {
        return "<label class=\"field\"><span>"+e(label)+"</span><input name=\""+e(name)+"\" type=\""+e(type)+"\" value=\""+e(value)+"\" "+extra+"></label>";
    }
    public static String area(String label,String name,Object value,int rows) {
        return "<label class=\"field\"><span>"+e(label)+"</span><textarea name=\""+e(name)+"\" rows=\""+rows+"\" required>"+e(value)+"</textarea></label>";
    }
    public static String select(String label,String name,List<Map<String,Object>> options,String field,Object chosen,boolean disabled) {
        StringBuilder b=new StringBuilder("<label class=\"field\"><span>"+e(label)+"</span><select name=\""+e(name)+"\" required "+(disabled?"disabled":"")+">");
        for(var m:options) {
            String id=Data.text(m,"id");
            b.append("<option value=\"").append(e(id)).append("\"").append(id.equals(String.valueOf(chosen))?" selected":"").append(">").append(e(m.get(field))).append("</option>");
        }
        b.append("</select></label>");
        if(disabled)b.append(hidden(name,chosen));
        return b.toString();
    }
    public static String icon(String name) {
        String d=switch(name) {
            case "home"->"M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z";
            case "pin"->"M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0z M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0";
            case "map"->"m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3z M9 3v15 M15 6v15";
            case "audio"->"M3 14v-3a9 9 0 0 1 18 0v3 M3 13h4v8H3z M17 13h4v8h-4z";
            case "search"->"M16 16l5 5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0";
            case "arrow"->"M4 12h16 M14 6l6 6-6 6";
            case "user"->"M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M4 21v-2a8 8 0 0 1 16 0v2";
            case "globe"->"M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 M2 12h20 M12 2c6 6 6 14 0 20-6-6-6-14 0-20";
            case "check"->"m5 12 4 4L20 5";
            case "plus"->"M12 4v16 M4 12h16";
            case "edit"->"m14 4 6 6 M4 20l5-1L21 7l-4-4L5 15z";
            case "clock"->"M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 M12 6v6l4 2";
            case "lock"->"M5 10h14v11H5z M8 10V6a4 4 0 0 1 8 0v4";
            case "cloud"->"M6 18H5a4 4 0 0 1-1-8 7 7 0 0 1 13-4 5 5 0 0 1 3 9 M12 21V11 m-4 4 4-4 4 4";
            case "book"->"M3 3h7l2 2 2-2h7v17h-7l-2 2-2-2H3z M12 5v17";
            default->"M12 3v18 M3 12h18";
        };
        return "<svg class=\"icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\""+d+"\"/></svg>";
    }
    public static String seconds(Object d) {
        try {
            int n=(int)Double.parseDouble(d.toString());
            return String.format("%d:%02d",n/60,n%60);
        }
        catch(Exception e) {
            return "--:--";
        }
    }
    public static String excerpt(Object t,int max) {
        String s=t==null?"":t.toString();
        return e(s.length()<=max?s:s.substring(0,max)+"…");
    }
}
