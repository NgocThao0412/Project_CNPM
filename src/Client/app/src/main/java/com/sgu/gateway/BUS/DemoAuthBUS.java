package com.sgu.gateway.BUS;
import com.sgu.core.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
/** Demonstration auth only. LIVE requests always delegate to the team's auth-service. */
public final class DemoAuthBUS {
    private final Map<String,Instant> sessions=new ConcurrentHashMap<>();
    private final Map<String,Deque<Instant>> attempts=new ConcurrentHashMap<>();
    public synchronized String login(Map<String,Object> m,String ip) {
        Instant now=Instant.now();
        Deque<Instant> a=attempts.computeIfAbsent(ip,k->new ArrayDeque<>());
        while(!a.isEmpty()&&a.peek().isBefore(now.minusSeconds(60)))a.remove();
        if(a.size()>=10)throw new Http.Error(429,"Thử lại sau một phút.");
        a.add(now);
        if(!"admin".equals(Data.text(m,"username").trim())||!"123456".equals(Data.text(m,"password")))throw new Http.Error(401,"Tên đăng nhập hoặc mật khẩu không đúng.");
        sessions.entrySet().removeIf(e->e.getValue().isBefore(now));
        String t=Http.token();
        sessions.put(t,now.plusSeconds(1800));
        return t;
    }
    public Map<String,Object> me(String t) {
        Instant end=sessions.get(t);
        if(end==null||end.isBefore(Instant.now())) {
            sessions.remove(t);
            throw new Http.Error(401,"Phiên quản trị đã hết hạn.");
        }
        return Data.row("id",1,"username","admin","fullName","Quản trị demo","email","admin@example.com");
    }
    public void logout(String t) {
        sessions.remove(t);
    }
}
