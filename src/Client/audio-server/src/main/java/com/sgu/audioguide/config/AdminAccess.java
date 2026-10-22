package com.sgu.audioguide.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Fail-closed session validation. Never trust a user-submitted adminId/header. */
@Component
public class AdminAccess {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    @Value("${services.auth.url:http://localhost:8081}") private String authUrl;
    public void requireAdmin(HttpServletRequest request) {
        String cookie=request.getHeader("Cookie");
        if(cookie==null || !cookie.contains("SGU_ADMIN_SESSION="))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập quản trị");
        try {
            HttpRequest r=HttpRequest.newBuilder(URI.create(authUrl+"/api/auth/me"))
                .header("Cookie",cookie).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<Void> response=http.send(r,HttpResponse.BodyHandlers.discarding());
            if(response.statusCode()!=200) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên quản trị không hợp lệ");
        }catch(ResponseStatusException x){throw x;}
        catch(Exception x){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Không xác thực được với auth-server");}
    }
}
