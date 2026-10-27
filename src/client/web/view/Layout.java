package web.view;

import core.Data;
import core.Settings;
import web.DTO.ClientSessionDTO;
import static web.view.Html.*;

/** The same Java-rendered layout works on desktop and mobile browsers. */
public final class Layout {
    private final Settings settings;

    public Layout(Settings settings) { this.settings = settings; }

    public String page(ClientSessionDTO session, String title, String tab,
                       String content, String notice) {
        boolean management = "admin".equals(tab) || "status".equals(tab) || "login".equals(tab);
        boolean adminArea = session != null && session.loggedIn()
                && ("admin".equals(tab) || "status".equals(tab));
        boolean trackVisitor = !management && (session == null || !session.loggedIn());
        String brand = link("/",
                "<img src=\"/assets/logo-sgu.png\" alt=\"Logo SGU\""
                + " class=\"brand-logo\" width=\"52\" height=\"52\">"
                + "<span class=\"brand-school-wrap\">"
                + "<strong class=\"brand-school\">Trường Đại học Sài Gòn</strong>"
                + "</span>", "brand brand-left");

        String center = management ? ""
                : "<div class=\"header-center\">"
                + link("/", "Saigon Audio Guide", "header-title")
                + "<span class=\"header-audio-mark\" aria-hidden=\"true\">"
                + "<span></span><span></span><span></span><span></span><span></span>"
                + "</span></div>"
                + "<nav class=\"desktop-nav header-right\" aria-label=\"Điều hướng chính\">"
                + nav("/", "Khám phá", "home", tab)
                + nav("/places", "Địa điểm", "places", tab)
                + nav("/map", "Bản đồ", "map", tab)
                + "</nav>";

        String back = "";
        if (adminArea && session.canGoBackInAdmin()) {
            back = "<div class=\"page-back-wrap\">"
                    + link("/admin/back", "<span aria-hidden=\"true\">←</span> Quay lại", "page-back")
                    + "</div>";
        } else if (!adminArea && ("login".equals(tab) || "status".equals(tab))) {
            back = "<div class=\"page-back-wrap\">"
                    + link("/", "<span aria-hidden=\"true\">←</span> Quay lại", "page-back")
                    + "</div>";
        }

        String noticeHtml = notice == null || notice.isBlank() ? ""
                : "<div role=\"status\" class=\"notice\">"
                    + icon("check") + e(notice) + "</div>";

        String pageContent;
        if (adminArea) {
            String current = "overview";
            if ("status".equals(tab)) current = "status";
            else if (title.contains("POI")) current = "pois";
            else if ("Người dùng".equals(title)) current = "users";
            else if ("Quản lý audio".equals(title)) current = "audio";
            else if (title.contains("thuyết minh") || title.contains("Nội dung")) current = "contents";
            String person = adminName(session);
            pageContent = "<main id=\"main\" class=\"page admin-shell\">"
                    + "<aside id=\"admin-sidebar\" class=\"admin-sidebar\" aria-label=\"Điều hướng quản trị\">"
                    + "<div class=\"sidebar-heading\"><span class=\"sidebar-heading-icon\">"
                    + icon("audio") + "</span><span>QUẢN TRỊ SGU<small>Campus Audio Guide</small></span></div>"
                    + "<nav class=\"sidebar-links\">"
                    + sidebar("/admin", "Tổng quan", "home", "overview", current)
                    + sidebar("/admin/pois", "Quản lý địa điểm", "pin", "pois", current)
                    + sidebar("/admin/contents", "Nội dung thuyết minh", "book", "contents", current)
                    + sidebar("/admin/audio", "Quản lý audio", "audio", "audio", current)
                    + sidebar("/admin/users", "Người dùng", "user", "users", current)
                    + sidebar("/status", "Trạng thái kết nối", "cloud", "status", current)
                    + "</nav>"
                    + "<div class=\"sidebar-signature\">Saigon Audio Guide<small>Trường Đại học Sài Gòn</small></div>"
                    + "</aside>"
                    + "<div class=\"admin-workspace\">"
                    + "<div class=\"admin-topbar\">"
                    + "<div class=\"admin-topbar-heading\">"
                    + "<button type=\"button\" class=\"admin-menu-toggle\""
                    + " aria-controls=\"admin-sidebar\" aria-expanded=\"true\""
                    + " aria-label=\"Ẩn hoặc hiện menu quản trị\">"
                    + "<span aria-hidden=\"true\">☰</span></button>"
                    + "<img src=\"/assets/logo-sgu.png\" alt=\"\""
                    + " class=\"admin-bar-logo\" width=\"38\" height=\"38\">"
                    + "<span>QUẢN TRỊ SGU"
                    + "<small>Saigon Audio Guide</small></span></div>"
                    + "<div class=\"admin-topbar-actions\"><span class=\"admin-user\">"
                    + icon("user") + "<span>Quản trị viên: <strong>" + e(person) + "</strong></span></span>"
                    + "<form method=\"post\" action=\"/logout\">"
                    + hidden("csrf", session.csrf)
                    + "<button class=\"admin-logout\" type=\"submit\">Đăng xuất</button></form>"
                    + "</div></div>"
                    + "<div class=\"admin-workspace-body\">" + back + noticeHtml + content + "</div>"
                    + "</div></main>";
        } else {
            pageContent = "<main id=\"main\" class=\"page\">" + back + noticeHtml
                    + content + "</main>";
        }

        return "<!doctype html><html lang=\"vi\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<meta name=\"theme-color\" content=\"#1262cb\">"
                + "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">"
                + "<title>" + e(title) + " | Saigon Audio Guide</title>"
                + "<link rel=\"icon\" href=\"/assets/logo-sgu.png\" type=\"image/png\">"
                + "<link rel=\"stylesheet\" href=\"/assets/app.css?v=visitors-20261023\">"
                + (adminArea
    ? "<link rel=\"stylesheet\" href=\"/assets/admin-theme.css?v=2\">"
      + "<script defer src=\"/assets/admin-shell.js?v=2\"></script>"
    : "")
                + (adminArea ? "<script defer src=\"/assets/admin-visitors.js\"></script>" :
                    trackVisitor ? "<script defer src=\"/assets/visitor-activity.js\"></script>" : "")
                + "</head><body class=\"" + (adminArea ? "admin-area" : management ? "management-page" : "visitor-page")
                + "\"" + (trackVisitor ? " data-visitor-activity=\"true\"" : "") + ">"
                + "<a class=\"skip\" href=\"#main\">Đến nội dung</a>"
                + "<header class=\"site-header site-header--centered"
                + (management ? " site-header--minimal" : "") + "\">"
                + "<div class=\"header-inner\"><div class=\"header-left\">" + brand + "</div>"
                + center + "</div></header>"
                + pageContent
                + (adminArea ? "" : "<footer class=\"site-footer\"><strong>Saigon Audio Guide</strong>"
                    + "<span>Đồ án Công nghệ phần mềm · Nhóm 12</span></footer>")
                + (management ? "" : "<nav class=\"bottom-nav\" aria-label=\"Điều hướng mobile\">"
                    + mobile("/", "Khám phá", "home", tab, "home")
                    + mobile("/places", "Địa điểm", "places", tab, "pin")
                    + mobile("/map", "Bản đồ", "map", tab, "map") + "</nav>")
                + "</body></html>";
    }

    public static String adminName(ClientSessionDTO session) {
        if (session == null || session.admin == null) return "Quản trị viên";
        String fullName = Data.text(session.admin, "fullName").trim();
        String username = Data.text(session.admin, "username").trim();
        if (fullName.isEmpty() || fullName.toLowerCase(java.util.Locale.ROOT).contains("demo")) {
            fullName = username;
        }
        return fullName.isEmpty() ? "Quản trị viên" : fullName;
    }

    private String sidebar(String href, String label, String symbol, String key, String active) {
        return link(href, icon(symbol) + "<span>" + label + "</span>",
                "sidebar-link" + (key.equals(active) ? " active" : ""));
    }
    private String nav(String href, String label, String key, String current) {
        return link(href, e(label), key.equals(current) ? "nav-link active" : "nav-link");
    }
    private String mobile(String href, String label, String key, String current, String ic) {
        return link(href, icon(ic) + "<span>" + e(label) + "</span>",
                key.equals(current) ? "active" : "");
    }

    /** Sidebar and logout are supplied by page(), so editor pages need no extra header. */
    public String adminNav(ClientSessionDTO session, String active) { return ""; }

    public String error(String message) {
        return "<div class=\"error-box\" role=\"alert\"><strong>Chưa thể hoàn tất thao tác</strong>"
                + "<p>" + e(message) + "</p></div>";
    }
}
