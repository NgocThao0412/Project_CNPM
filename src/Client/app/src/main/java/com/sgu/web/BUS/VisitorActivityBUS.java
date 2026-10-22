package com.sgu.web.BUS;

import com.sgu.core.Data;
import com.sgu.core.Http;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local DEMO simulator of the team's token-based visitor-service contract.
 * LIVE traffic is sent to the separate visitor-service, never counted here.
 */
public final class VisitorActivityBUS {
    public static final long TTL_MILLIS = 30_000L;
    private static final int MAX_SESSIONS = 10_000;
    private final ConcurrentHashMap<String, Visit> sessions = new ConcurrentHashMap<>();

    private static final class Visit {
        volatile long seen;
        volatile boolean listening;
        Visit(long now) { seen=now; }
    }

    public synchronized String create() {
        prune();
        if (sessions.size() >= MAX_SESSIONS)
            throw new Http.Error(503, "Chế độ Demo đã đạt giới hạn phiên.");
        String token=UUID.randomUUID().toString();
        sessions.put(token,new Visit(System.currentTimeMillis()));
        return token;
    }
    public synchronized void heartbeat(String token, boolean listening) {
        prune();
        Visit visit=sessions.get(token);
        if(visit==null)throw new Http.Error(401,"Phiên khách hết hạn, cần tạo lại.");
        visit.seen=System.currentTimeMillis();
        visit.listening=listening;
    }
    public synchronized void end(String token) {
        prune();
        sessions.remove(token);
    }
    public static boolean expired(long lastSeen,long now) {
        return now-lastSeen>TTL_MILLIS;
    }
    private void prune() {
        long now=System.currentTimeMillis();
        sessions.entrySet().removeIf(row -> expired(row.getValue().seen,now));
    }
    public synchronized Map<String,Object> stats() {
        prune();
        long listening=sessions.values().stream().filter(v -> v.listening).count();
        return Data.row("activeVisitors",sessions.size(),"activeListeners",listening);
    }
}
