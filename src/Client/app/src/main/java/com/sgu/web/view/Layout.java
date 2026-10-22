package com.sgu.web.view;

import com.sgu.core.Settings;
import com.sgu.web.DTO.ClientSessionDTO;
import static com.sgu.web.view.Html.*;

/** Shared Java-rendered layout for desktop and mobile browsers. */
public final class Layout {
    private final Settings settings;

    public Layout(Settings settings) {
        this.settings = settings;
    }

    public String page(ClientSessionDTO session, String title, String tab,
                       String content, String notice) {
        boolean management = "admin".equals(tab) || "status".equals(tab)
                || "login".equals(tab);
        String brand = link("/",
                "<img src=\"/assets/logo-sgu.png\" alt=\"Logo SGU\""
                + " class=\"brand-logo\" width=\"52\" height=\"52\">"
                + "<span class=\"brand-school-wrap\">"
                + "<strong class=\"brand-school\">Trường Đại học Sài Gòn</strong>"
                + "</span>",
                "brand brand-left");

        // Three balanced header areas: the university at left, app title in the
        // middle, and visitor navigation at right (no visitor links in admin).
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
        if (management) {
            String backTo = "admin".equals(tab) && !"Quản trị".equals(title)
                    ? "/admin" : "/";
            if ("status".equals(tab) && session != null && session.loggedIn()) {
                backTo = "/admin";
            }
            back = "<div class=\"page-back-wrap\">"
                    + link(backTo, "<span aria-hidden=\"true\">←</span> Quay lại", "page-back")
                    + "</div>";
        }

        return "<!doctype html><html lang=\"vi\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<meta name=\"theme-color\" content=\"#1262cb\">"
                + "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">"
                + "<title>" + e(title) + " | Saigon Audio Guide</title>"
                + "<link rel=\"icon\" href=\"/assets/logo-sgu.png\" type=\"image/png\">"
                + "<link rel=\"stylesheet\" href=\"/assets/app.css?v=saigon-audio-guide-hero-20261023\">"
                + "</head><body class=\"" + (management ? "management-page" : "visitor-page")
                + "\" data-ui-version=\"saigon-audio-guide-hero-20261023\">"
                + "<a class=\"skip\" href=\"#main\">Đến nội dung</a>"
                + "<header class=\"site-header site-header--centered"
                + (management ? " site-header--minimal" : "") + "\">"
                + "<div class=\"header-inner\"><div class=\"header-left\">" + brand + "</div>"
                + center + "</div></header>"
                + "<main id=\"main\" class=\"page\">" + back
                + (notice == null || notice.isBlank() ? ""
                    : "<div role=\"status\" class=\"notice\">" + icon("check") + e(notice) + "</div>")
                + content + "</main>"
                + "<footer class=\"site-footer\"><strong>Saigon Audio Guide</strong>"
                + "<span>Đồ án Công nghệ phần mềm · Nhóm 12</span><span>"
                + (settings.demo()
                    ? "Nội dung, tọa độ và giọng đọc demo chỉ để kiểm thử."
                    : "Dữ liệu được lấy qua các API Java.")
                + "</span></footer>"
                + (management ? "" : "<nav class=\"bottom-nav\" aria-label=\"Điều hướng mobile\">"
                    + mobile("/", "Khám phá", "home", tab, "home")
                    + mobile("/places", "Địa điểm", "places", tab, "pin")
                    + mobile("/map", "Bản đồ", "map", tab, "map") + "</nav>")
                + "</body></html>";
    }

    private String nav(String href, String label, String key, String current) {
        return link(href, e(label), key.equals(current) ? "nav-link active" : "nav-link");
    }

    private String mobile(String href, String label, String key, String current, String ic) {
        return link(href, icon(ic) + "<span>" + e(label) + "</span>",
                key.equals(current) ? "active" : "");
    }

    public String adminNav(ClientSessionDTO session, String active) {
        return "<div class=\"admin-header\"><div><span class=\"eyebrow\">WORKSPACE</span>"
                + "<h1>Không gian quản trị</h1><p>Quản lý địa điểm và câu chuyện của SGU.</p></div>"
                + "<form method=\"post\" action=\"/logout\">" + hidden("csrf", session.csrf)
                + "<button class=\"btn btn-light\" type=\"submit\">Đăng xuất</button></form></div>"
                + "<nav class=\"tabs\">"
                + link("/admin", icon("home") + "Tổng quan", "overview".equals(active) ? "active" : "")
                + link("/admin/pois", icon("pin") + "Quản lý POI", "pois".equals(active) ? "active" : "")
                + link("/admin/contents", icon("book") + "Nội dung & audio", "contents".equals(active) ? "active" : "")
                + "</nav>";
    }

    public String error(String message) {
        return "<div class=\"error-box\" role=\"alert\"><strong>Chưa thể hoàn tất thao tác</strong>"
                + "<p>" + e(message) + "</p></div>";
    }
}
