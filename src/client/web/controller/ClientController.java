            return;
        } throw new Http.Error(404,"Trang không tồn tại.");
    } private void visitorApi(HttpExchange x,String path,String method)throws Exception {
        if(!method.equals("POST"))throw new Http.Error(405,"Method not allowed");
        // Same-origin browser POSTs only. Never proxy arbitrary visitor headers.
        String site=x.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if(site!=null && !site.equals("same-origin") && !site.equals("none"))
            throw new Http.Error(403,"Chỉ chấp nhận yêu cầu cùng nguồn.");
        String origin=x.getRequestHeaders().getFirst("Origin");
        String host=x.getRequestHeaders().getFirst("Host");
        if(origin!=null && !origin.isBlank()) {
            try {
                URI u=URI.create(origin);
                if(!Set.of("http","https").contains(u.getScheme()) ||
                        host==null || !host.equalsIgnoreCase(u.getAuthority()))
                    throw new Http.Error(403,"Nguồn gửi không hợp lệ.");
            } catch(IllegalArgumentException e) {
                throw new Http.Error(403,"Nguồn gửi không hợp lệ.");
            }
        }
        if(path.equals("/api/visitor/sessions")) {
            // A brand-new anonymous session is issued by visitor-service, not a browser UUID.
            Http.json(x,200,Data.row("token",activity.create()));
            return;
        }
        String token=activity.checkedToken(x.getRequestHeaders().getFirst("X-Visitor-Token"));
        if (path.equals("/api/visitor/listening-events")) {
    String contentType = x.getRequestHeaders()
            .getFirst("Content-Type");

    if (contentType == null
            || !contentType.toLowerCase(Locale.ROOT)
                    .startsWith("application/json")) {
        throw new Http.Error(415, "Yêu cầu phải là JSON.");
    }

    byte[] body = x.getRequestBody().readNBytes(2049);

    if (body.length > 2048) {
        throw new Http.Error(413, "Dữ liệu lượt nghe quá lớn.");
    }

    Map<String, Object> value = Json.object(
            new String(body, StandardCharsets.UTF_8)
    );

    if (!(value.get("eventId") instanceof String eventId)
            || !eventId.matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}"
                    + "-[0-9a-f]{4}-[0-9a-f]{12}"
            )) {
        throw new Http.Error(400, "Mã sự kiện không hợp lệ.");
    }

    if (!(value.get("audioFileId") instanceof Number number)
            || !Double.isFinite(number.doubleValue())
            || number.doubleValue() != number.longValue()
            || number.longValue() <= 0
            || number.doubleValue() > 9_007_199_254_740_991d) {
        throw new Http.Error(400, "ID audio không hợp lệ.");
    }

    activity.recordListening(
            token,
            eventId,
            number.longValue()
    );

    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.sendResponseHeaders(204, -1);
    return;
}
        if(path.equals("/api/visitor/heartbeat")) {
            String contentType=x.getRequestHeaders().getFirst("Content-Type");
            if(contentType==null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json"))
                throw new Http.Error(415,"Yêu cầu phải là JSON.");
            byte[] body=x.getRequestBody().readNBytes(1025);
            if(body.length>1024)throw new Http.Error(413,"Heartbeat quá lớn.");
            Map<String,Object> value=Json.object(new String(body,StandardCharsets.UTF_8));
            if(!(value.get("listening") instanceof Boolean listening))
                throw new Http.Error(400,"Trường listening phải là true hoặc false.");
            activity.heartbeat(token,listening);
        } else {
            activity.end(token);
        }
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.sendResponseHeaders(204,-1);
    } private void asset(HttpExchange x,String path)throws Exception {
        if(!path.matches("/assets/[a-z0-9.-]+\\.(css|svg|png|js)"))throw new Http.Error(404,"Asset not found");
        try(var in=ClientController.class.getResourceAsStream(path)) {
            if(in==null)throw new Http.Error(404,"Asset not found");
            Http.bytes(x,200,path.endsWith("css")?"text/css; charset=utf-8":path.endsWith("js")?
                "application/javascript; charset=utf-8":path.endsWith("png")?"image/png":"image/svg+xml",in.readAllBytes());
        }
    } private void poiImage(HttpExchange x,ClientSessionDTO ss,long id)throws Exception {
        try {
            var p=ClientBUS.byId(api.pois(ss,ss.loggedIn()),id);
            String u=Data.text(p,"imageUrl");
            URI base=URI.create(s.get("poi.url","http://127.0.0.1:8082")),uri=base.resolve(u);
            boolean samePort=uri.getPort()==base.getPort();
            boolean sameHost=Objects.equals(uri.getHost(),base.getHost())||Set.of("localhost","127.0.0.1").contains(uri.getHost())&&Set.of("localhost","127.0.0.1").contains(base.getHost());
            if(!sameHost||!samePort||!Set.of("http","https").contains(uri.getScheme()))throw new IllegalArgumentException();
            Http.Reply r=Http.request("GET",uri.toString(),"",null,null,Map.of());
            if(r.status()!=200||!r.header("Content-Type").startsWith("image/")||r.body().length>8*1024*1024)throw new IllegalArgumentException();
            Http.bytes(x,200,r.header("Content-Type"),r.body());
        }
        catch(Exception e) {
            asset(x,"/assets/campus-"+((id-1)%3)+".svg");
        }
    }
}
