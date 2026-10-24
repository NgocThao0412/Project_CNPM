package web.DTO;
import core.*;
import java.time.*;
import java.util.*;
public final class ClientSessionDTO {
    public final String id=Http.token();
    public final String csrf=Http.token();
    public volatile String upstreamCookie="";
    public volatile Map<String,Object> admin=null;
    public volatile Instant touched=Instant.now();
    // Only administrator URLs are stored. A Back click can never leave the admin area.
    private final Deque<String> adminHistory = new ArrayDeque<>();
    private String currentAdminPage = "";

    public synchronized void rememberAdminPage(String url) {
        if (url == null || !(url.equals("/admin") || url.startsWith("/admin/")
                || url.equals("/status"))) return;
        if (url.equals(currentAdminPage)) return; // Refresh must not add history.
        if (!currentAdminPage.isEmpty()) {
            if (!adminHistory.isEmpty() && url.equals(adminHistory.peekLast())) {
                adminHistory.removeLast();
            } else {
                adminHistory.addLast(currentAdminPage);
                while (adminHistory.size() > 40) adminHistory.removeFirst();
            }
        }
        currentAdminPage = url;
    }

    public synchronized boolean canGoBackInAdmin() {
        return !adminHistory.isEmpty() || (!currentAdminPage.isEmpty()
                && !currentAdminPage.equals("/admin"));
    }

    public synchronized String goBackInAdmin() {
        String previous = adminHistory.pollLast();
        if (previous == null) previous = "/admin";
        currentAdminPage = previous; // Redirect back must not re-add the page we left.
        return previous;
    }

    public boolean loggedIn() {
        return admin!=null&&!upstreamCookie.isEmpty();
    }
}
