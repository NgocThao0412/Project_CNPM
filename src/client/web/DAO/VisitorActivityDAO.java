package web.DAO;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import core.Data;
import core.Http;
import core.Json;
import core.Settings;
import web.DTO.ClientSessionDTO;

public final class VisitorActivityDAO {

    private final Settings settings;

    public VisitorActivityDAO(Settings settings) {
        this.settings = settings;
    }

    private String endpoint(String path) {
        return settings.get(
                "visitor.url",
                "http://127.0.0.1:8085"
        ) + path;
    }

    public String create() throws Exception {
        Map<String, Object> response = Data.map(
                Http.call(
                        "POST",
                        endpoint("/api/visitor/sessions"),
                        "",
                        null
                ).require().json()
        );

        return checkedToken(Data.text(response, "token"));
    }

    public void heartbeat(
            String token,
            boolean listening
    ) throws Exception {
        token = checkedToken(token);

        byte[] body = Json.stringify(
                Data.row("listening", listening)
        ).getBytes(StandardCharsets.UTF_8);

        Http.request(
                "POST",
                endpoint("/api/visitor/heartbeat"),
                "",
                "application/json",
                body,
                Map.of("X-Visitor-Token", token)
        ).require();
    }

    public void end(String token) throws Exception {
        token = checkedToken(token);

        Http.request(
                "POST",
                endpoint("/api/visitor/end"),
                "",
                null,
                null,
                Map.of("X-Visitor-Token", token)
        ).require();
    }

    public Map<String, Object> stats(
            ClientSessionDTO admin
    ) throws Exception {
        if (admin == null || !admin.loggedIn()) {
            throw new Http.Error(
                    401,
                    "Vui lòng đăng nhập quản trị."
            );
        }

        Map<String, Object> response = Data.map(
                Http.call(
                        "GET",
                        endpoint("/api/admin/visitor-stats"),
                        admin.upstreamCookie,
                        null
                ).require().json()
        );

        int visitors = nonnegative(
                response.get("activeVisitors"),
                "activeVisitors"
        );

        int listeners = nonnegative(
                response.get("activeListeners"),
                "activeListeners"
        );

        if (listeners > visitors) {
            throw new Http.Error(
                    502,
                    "Số phiên đang nghe vượt tổng phiên truy cập."
            );
        }

        return Data.row(
                "activeVisitors", visitors,
                "activeListeners", listeners
        );
    }

    private static int nonnegative(
            Object value,
            String field
    ) {
        if (!(value instanceof Number number)
                || number.longValue() != number.doubleValue()
                || number.longValue() < 0
                || number.longValue() > Integer.MAX_VALUE) {
            throw new Http.Error(
                    502,
                    "Phản hồi visitor-service thiếu hoặc sai trường " + field
            );
        }

        return number.intValue();
    }

    public static String checkedToken(String token) {
        if (token == null
                || !token.matches("[A-Za-z0-9._~-]{8,512}")) {
            throw new Http.Error(
                    400,
                    "Visitor token không hợp lệ."
            );
        }

        return token;
    }
}