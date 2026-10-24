package client;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthServiceClient {

    private static final String COOKIE_NAME = "SGU_ADMIN_SESSION";

    private final RestClient restClient;

    public AuthServiceClient(
            @Value("${services.auth.base-url}") String authBaseUrl
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(authBaseUrl)
                .requestFactory(factory)
                .build();
    }

    public Long requireAdmin(HttpServletRequest request) {
        String sessionId = findSessionId(request);

        if (sessionId == null
                || !sessionId.matches("[A-Za-z0-9._-]{1,256}")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        try {
            AdminIdentity admin = restClient.get()
                    .uri("/api/auth/me")
                    .header(
                            HttpHeaders.COOKIE,
                            COOKIE_NAME + "=" + sessionId
                    )
                    .retrieve()
                    .body(AdminIdentity.class);

            if (admin == null || admin.id() == null || admin.id() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập không hợp lệ"
                );
            }

            return admin.id();

        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
            );

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa thể xác thực tài khoản, vui lòng thử lại"
            );
        }
    }

    private String findSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public record AdminIdentity(
            Long id,
            String username,
            String fullName,
            String email
    ) {
    }
}