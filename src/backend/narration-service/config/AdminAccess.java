
package config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AdminAccess {

    private static final String COOKIE_NAME = "SGU_ADMIN_SESSION";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Value("${services.auth.url:http://localhost:8081}")
    private String authUrl;

    public void requireAdmin(HttpServletRequest request) {
        String sessionId = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        if (sessionId == null
                || !sessionId.matches("[A-Za-z0-9._-]{1,256}")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập quản trị"
            );
        }

        try {
            HttpRequest authRequest = HttpRequest.newBuilder(
                            URI.create(authUrl + "/api/auth/me"))
                    .header("Cookie", COOKIE_NAME + "=" + sessionId)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = http.send(
                    authRequest,
                    HttpResponse.BodyHandlers.discarding()
            );

            int status = response.statusCode();

            if (status == 200) {
                return;
            }

            if (status == 401 || status == 403) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên quản trị không hợp lệ"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Auth Service chưa thể xác thực"
            );

        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Yêu cầu xác thực bị gián đoạn",
                    exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Không kết nối được Auth Service",
                    exception
            );
        }
    }
}
