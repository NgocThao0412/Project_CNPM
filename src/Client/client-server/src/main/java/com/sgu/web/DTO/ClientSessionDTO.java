package com.sgu.web.DTO;
import com.sgu.core.*;
import java.time.*;
import java.util.*;
public final class ClientSessionDTO {
    public final String id=Http.token();
    public final String csrf=Http.token();
    public volatile String upstreamCookie="";
    public volatile Map<String,Object> admin=null;
    public volatile Instant touched=Instant.now();
    public boolean loggedIn() {
        return admin!=null&&!upstreamCookie.isEmpty();
    }
}
