package core;
import java.util.*;
import java.lang.reflect.RecordComponent;
/** Small strict JSON codec for DTOs. No eval, polymorphic deserialization or reflection on inputs. */
public final class Json {
    private Json() {
    }
    public static Object parse(String text) {
        Parser p=new Parser(text);
        Object v=p.value(0);
        p.ws();
        if(p.i!=text.length())throw new IllegalArgumentException("Unexpected trailing JSON");
        return v;
    } @SuppressWarnings("unchecked") public static Map<String,Object> object(String text) {
        Object v=parse(text);
        if(!(v instanceof Map))throw new IllegalArgumentException("Expected JSON object");
        return (Map<String,Object>)v;
    }
    public static String stringify(Object v) {
        StringBuilder b=new StringBuilder();
        append(b,v);
        return b.toString();
    } private static void append(StringBuilder b,Object v) {
        if(v==null) {
            b.append("null");
            return;
        } if(v instanceof String || v instanceof Enum<?>) {
            quote(b,v.toString());
            return;
        } if(v instanceof Number n) {
            if(n instanceof Double d && !Double.isFinite(d)||n instanceof Float f && !Float.isFinite(f))throw new IllegalArgumentException("Non-finite number");
            b.append(n);
            return;
        } if(v instanceof Boolean) {
            b.append(v);
            return;
        } if(v instanceof Map<?,?> m) {
            b.append('{');
            boolean first=true;
            for(var e:m.entrySet()) {
                if(!first)b.append(',');
                first=false;
                quote(b,e.getKey().toString());
                b.append(':');
                append(b,e.getValue());
            }
            b.append('}');
            return;
        } if(v instanceof Iterable<?> list) {
            b.append('[');
            boolean first=true;
            for(Object x:list) {
                if(!first)b.append(',');
                first=false;
                append(b,x);
            }
            b.append(']');
            return;
        } if(v.getClass().isRecord()) {
            Map<String,Object> m=new LinkedHashMap<>();
            try {
                for(RecordComponent c:v.getClass().getRecordComponents())m.put(c.getName(),c.getAccessor().invoke(v));
            }
            catch(Exception e) {
                throw new IllegalArgumentException(e);
            }
            append(b,m);
            return;
        } throw new IllegalArgumentException("Unsupported JSON type "+v.getClass().getSimpleName());
    } private static void quote(StringBuilder b,String s) {
        b.append('"');
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"'->b.append("\\\"");
                case '\\'->b.append("\\\\");
                case '\n'->b.append("\\n");
                case '\r'->b.append("\\r");
                case '\t'->b.append("\\t");
                default-> {
                    if(c<32)b.append(String.format("\\u%04x",(int)c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
    } private static final class Parser {
        final String s;
        int i;
        Parser(String s) {
            this.s=Objects.requireNonNull(s);
        } void ws() {
            while(i<s.length()&&" \n\r\t".indexOf(s.charAt(i))>=0)i++;
        } char ch() {
            if(i>=s.length())throw new IllegalArgumentException("Unexpected end of JSON");
            return s.charAt(i);
        } void take(char c) {
            ws();
            if(ch()!=c)throw new IllegalArgumentException("Expected "+c);
            i++;
        } Object value(int depth) {
            if(depth>50)throw new IllegalArgumentException("JSON too deep");
            ws();
            char c=ch();
            if(c=='"')return str();
            if(c=='{') {
                i++;
                Map<String,Object> m=new LinkedHashMap<>();
                ws();
                if(ch()=='}') {
                    i++;
                    return m;
                }
                while(true) {
                    ws();
                    String k=str();
                    take(':');
                    Object v=value(depth+1);
                    if(m.containsKey(k))throw new IllegalArgumentException("Duplicate JSON key");
                    m.put(k,v);
                    ws();
                    c=ch();
                    i++;
                    if(c=='}')return m;
                    if(c!=',')throw new IllegalArgumentException("Expected comma");
                }
            } if(c=='[') {
                i++;
                List<Object> a=new ArrayList<>();
                ws();
                if(ch()==']') {
                    i++;
                    return a;
                }
                while(true) {
                    a.add(value(depth+1));
                    ws();
                    c=ch();
                    i++;
                    if(c==']')return a;
                    if(c!=',')throw new IllegalArgumentException("Expected comma");
                }
            } for(String t:List.of("true","false","null")) {
                if(s.startsWith(t,i)) {
                    i+=t.length();
                    return t.equals("null")?null:Boolean.valueOf(t);
                }
            } int start=i;
            while(i<s.length()&&"-+0123456789.eE".indexOf(s.charAt(i))>=0)i++;
            String n=s.substring(start,i);
            if(!n.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw new IllegalArgumentException("Invalid JSON number");
            try {
                return n.contains(".")||n.contains("e")||n.contains("E")?new java.math.BigDecimal(n):Long.valueOf(n);
            }
            catch(NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number");
            }
        } String str() {
            take('"');
            StringBuilder b=new StringBuilder();
            while(i<s.length()) {
                char c=s.charAt(i++);
                if(c=='"')return b.toString();
                if(c<32)throw new IllegalArgumentException("JSON control character");
                if(c=='\\') {
                    if(i>=s.length())throw new IllegalArgumentException("Bad escape");
                    c=s.charAt(i++);
                    switch(c) {
                        case '"','\\','/'->b.append(c);
                        case 'b'->b.append('\b');
                        case 'f'->b.append('\f');
                        case 'n'->b.append('\n');
                        case 'r'->b.append('\r');
                        case 't'->b.append('\t');
                        case 'u'-> {
                            if(i+4>s.length())throw new IllegalArgumentException("Bad Unicode");
                            try {
                                b.append((char)Integer.parseInt(s.substring(i,i+4),16));
                            }
                            catch(NumberFormatException ex) {
                                throw new IllegalArgumentException("Bad Unicode");
                            }
                            i+=4;
                        }
                        default->throw new IllegalArgumentException("Bad JSON escape");
                    }
                }
                else b.append(c);
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }
    }
}
