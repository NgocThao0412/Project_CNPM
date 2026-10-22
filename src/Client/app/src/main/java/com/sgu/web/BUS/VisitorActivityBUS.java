package com.sgu.web.BUS;

import com.sgu.core.Data;
import com.sgu.core.Http;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary DEMO-only visitor activity tracking inside the Client process.
 * LIVE mode forwards events to the separate team's backend; no fake LIVE totals.
 * No IP address, real name or persistent user profile is retained here.
 */
public final class VisitorActivityBUS {
    public static final long TTL_MILLIS = 30_000L;
    private static final int MAX_DEMO_TABS = 10_000;
    private final ConcurrentHashMap<String, Visit> sessions = new ConcurrentHashMap<>();

    private static final class Visit {
        volatile long seen;
        volatile boolean listening;
        volatile String page;
        volatile long poiId;
        volatile long audioId;
        Visit(long now, String page, long poiId, long audioId, boolean listening) {
            this.seen=now; this.page=page; this.poiId=poiId;
            this.audioId=audioId; this.listening=listening;
        }
    }

    public static Map<String,Object> validate(Object value) {
        Map<String,Object> source=Data.map(value);
        String sessionId=Data.text(source,"sessionId");
        try {
            if (!UUID.fromString(sessionId).toString().equals(sessionId)) throw new IllegalArgumentException();
        } catch(Exception ex) { throw new Http.Error(400,"Mã phiên khách không hợp lệ."); }
        String event=Data.text(source,"event");
        if(!List.of("heartbeat","play","pause","ended","close").contains(event))
            throw new Http.Error(400,"Loại tín hiệu khách không hợp lệ.");
        String page=Data.text(source,"page");
        if(page.length()>150 || !page.startsWith("/") || page.startsWith("//") || page.contains("\\") || page.contains("?"))
            throw new Http.Error(400,"Đường dẫn trang không hợp lệ.");
        long poiId=number(source,"poiId"), audioId=number(source,"audioId");
        boolean listening=Data.bool(source,"listening",false);
        // The Browser's actual audio play/pause/ended events are authoritative for listening.
        if (event.equals("play")) listening=true;
        if (event.equals("pause") || event.equals("ended") || event.equals("close")) listening=false;
        return Data.row("sessionId",sessionId,"event",event,"page",page,
                "poiId",poiId,"audioId",audioId,"listening",listening);
    }

    private static long number(Map<String,Object> source,String key) {
        String v=Data.text(source,key);
        if(v.isBlank()) return 0;
        try {
            long n=Long.parseLong(v);
            if(n<0 || n>1_000_000_000L) throw new IllegalArgumentException();
            return n;
        } catch(Exception ex) { throw new Http.Error(400,"Mã "+key+" không hợp lệ."); }
    }

    public synchronized void accept(Map<String,Object> value) {
        String key=Data.text(value,"sessionId"), event=Data.text(value,"event");
        long now=System.currentTimeMillis();
        prune(now);
        if(event.equals("close")) { sessions.remove(key); return; }
        if(sessions.size()>=MAX_DEMO_TABS && !sessions.containsKey(key))
            throw new Http.Error(503,"Quá nhiều phiên thử nghiệm. Vui lòng thử sau.");
        long poiId=number(value,"poiId"),audioId=number(value,"audioId");
        boolean listening=Data.bool(value,"listening",false);
        // An explicit player event overrides a stale heartbeat.
        if(event.equals("play")) listening=true;
        if(event.equals("pause") || event.equals("ended")) listening=false;
        sessions.put(key,new Visit(now,Data.text(value,"page"),poiId,audioId,listening));
    }
    public static boolean expired(long lastHeartbeatMillis, long nowMillis) {
        return nowMillis-lastHeartbeatMillis>TTL_MILLIS;
    }
    private void prune(long now) {
        sessions.entrySet().removeIf(e -> expired(e.getValue().seen,now));
    }
    public synchronized Map<String,Object> summary() {
        long now=System.currentTimeMillis();
        prune(now);
        List<Map.Entry<String,Visit>> rows=new ArrayList<>(sessions.entrySet());
        rows.sort(Comparator.comparingLong((Map.Entry<String,Visit> e)->e.getValue().seen).reversed());
        long listening=rows.stream().filter(e->e.getValue().listening).count();
        List<Map<String,Object>> visitors=new ArrayList<>();
        for(var row:rows) {
            if(visitors.size()>=80) break;
            Visit v=row.getValue();
            Map<String,Object> display=new LinkedHashMap<>();
            display.put("visitorId",row.getKey().substring(0,8));
            display.put("status",v.listening?"listening":"browsing");
            display.put("poiId",v.poiId);
            display.put("audioId",v.audioId);
            display.put("page",v.page);
            display.put("secondsAgo",Math.max(0,(now-v.seen)/1000));
            visitors.add(display);
        }
        return Data.row("connected",true,"source","demo-memory","online",rows.size(),
                "listening",listening,"visitors",visitors,"updatedAt",Instant.ofEpochMilli(now).toString());
    }
}
