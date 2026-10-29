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
        return page(session, title, tab, content, notice, "vi");
    }

    public String page(ClientSessionDTO session, String title, String tab,
                       String content, String notice, String locale) {
        String selectedLanguage = UiLanguage.normalize(locale);
        boolean loginPage = "login".equals(tab);
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

        // The flag follows the existing selected UI language (VI / EN / ZH / KO).
        // This changes only the decoration below the centered title.
        String flagName = switch (selectedLanguage) {
            case "en" -> "Anh";
            case "zh" -> "Trung Quốc";
            case "ko" -> "Hàn Quốc";
            default -> "Việt Nam";
        };
        String waveBars = "<i></i><i></i><i></i><i></i><i></i>";
        String flagDecoration = "<div class=\"header-flag-decor\" role=\"img\" aria-label=\"Cờ "
                + flagName + " giữa hai dải sóng âm\">"
                + "<span class=\"header-wave header-wave--left\" aria-hidden=\"true\">"
                + waveBars + "</span>"
                + "<img class=\"header-waving-flag\" src=\"/assets/flag-"
                + selectedLanguage + ".svg\" alt=\"\" width=\"58\" height=\"38\">"
                + "<span class=\"header-wave header-wave--right\" aria-hidden=\"true\">"
                + waveBars + "</span></div>";

        String center = management ? ""
                : "<div class=\"header-center\">"
                + link("/", "Saigon Audio Guide", "header-title")
                + flagDecoration + "</div>"
                + "<nav class=\"desktop-nav header-right\" aria-label=\"Điều hướng chính\">"
                + nav("/", "Khám phá", "home", tab)
                + nav("/places", "Địa điểm", "places", tab)
                + nav("/map", "Bản đồ", "map", tab)
                + languagePicker(selectedLanguage, false)
                + "</nav>"
                + languagePicker(selectedLanguage, true);

        String back = "";
        if (adminArea && session.canGoBackInAdmin()) {
            back = "<div class=\"page-back-wrap\">"
                    + link("/admin/back", "<span aria-hidden=\"true\">←</span> Quay lại", "page-back")
                    + "</div>";
        } else if (!adminArea && "status".equals(tab)) {
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
                    + "<aside class=\"admin-sidebar\" aria-label=\"Điều hướng quản trị\">"
                    + "<div class=\"sidebar-heading\"><span class=\"sidebar-heading-icon\">"
                    + icon("audio") + "</span><span>QUẢN TRỊ SGU<small>Campus Audio Guide</small></span></div>"
                    + "<nav class=\"sidebar-links\">"
                    + sidebar("/admin", "Tổng quan", "home", "overview", current)
                    + sidebar("/admin/pois", "Quản lý địa điểm", "pin", "pois", current)
                    + sidebar("/admin/contents", "Nội dung thuyết minh", "book", "contents", current)
                    + sidebar("/admin/audio", "Quản lý audio", "audio", "audio", current)
                    + sidebar("/admin/users", "Thống kê", "user", "users", current)
                    + sidebar("/status", "Trạng thái kết nối", "cloud", "status", current)
                    + "</nav>"
                    + "<div class=\"sidebar-signature\">Saigon Audio Guide<small>Trường Đại học Sài Gòn</small></div>"
                    + "</aside>"
                    + "<div class=\"admin-workspace\">"
                    + "<div class=\"admin-topbar\"><div class=\"admin-topbar-heading\">"
                    + icon("audio") + "<span>HỆ THỐNG QUẢN TRỊ<small>Saigon Audio Guide · SGU</small></span></div>"
                    + "<div class=\"admin-topbar-actions\"><span class=\"admin-user\">"
                    + icon("user") + "<span>Quản trị viên: <strong>" + e(person) + "</strong></span></span>"
                    + "<form method=\"post\" action=\"/logout\">"
                    + hidden("csrf", session.csrf)
                    + "<button class=\"admin-logout\" type=\"submit\">Đăng xuất</button></form>"
                    + "</div></div>"
                    + "<div class=\"admin-workspace-body\">" + back + noticeHtml + content + "</div>"
                    + "</div></main>";
        } else if (loginPage) {
            // The login is a standalone centered card; other pages retain their header/back links.
            pageContent = "<main id=\"main\" class=\"page login-page-shell\">"
                    + noticeHtml + content + "</main>";
        } else {
            pageContent = "<main id=\"main\" class=\"page\">" + back + noticeHtml
                    + content + "</main>";
        }

        String html = "<!doctype html><html lang=\"" + selectedLanguage + "\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<meta name=\"theme-color\" content=\"#1262cb\">"
                + "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">"
                + "<title>" + e(title) + " | Saigon Audio Guide</title>"
                + "<link rel=\"icon\" href=\"/assets/logo-sgu.png\" type=\"image/png\">"
                + "<link rel=\"stylesheet\" href=\"/assets/app.css?v=flagwave-20261029\">"
                + (adminArea ? "<link rel=\"stylesheet\" href=\"/assets/admin-theme.css?v=restore-20261030\">" : "")
                + (adminArea ? "<script defer src=\"/assets/admin-shell.js?v=restore-20261030\"></script>" : "")
                + (!management ? "<script defer src=\"/assets/visitor-language.js\"></script>" : "")
                + (adminArea ? "<script defer src=\"/assets/admin-visitors.js\"></script>" :
                    trackVisitor ? "<script defer src=\"/assets/visitor-activity.js\"></script>" : "")
                + "</head><body class=\"" + (loginPage ? "login-page" : adminArea ? "admin-area" : management ? "management-page" : "visitor-page")
                + "\"" + (trackVisitor ? " data-visitor-activity=\"true\"" : "") + ">"
                + "<a class=\"skip\" href=\"#main\">Đến nội dung</a>"
                + (loginPage ? "" : "<header class=\"site-header site-header--centered"
                + (management ? " site-header--minimal" : "") + "\">"
                + "<div class=\"header-inner\"><div class=\"header-left\">" + brand + "</div>"
                + center + "</div></header>")
                + pageContent
                + (adminArea || loginPage ? "" : "<footer class=\"site-footer\"><strong>Saigon Audio Guide</strong>"
                    + "<span>© 2026 Saigon Audio Guide. All rights reserved.</span></footer>")
                + (management ? "" : "<nav class=\"bottom-nav\" aria-label=\"Điều hướng mobile\">"
                    + mobile("/", "Khám phá", "home", tab, "home")
                    + mobile("/places", "Địa điểm", "places", tab, "pin")
                    + mobile("/map", "Bản đồ", "map", tab, "map") + "</nav>")
                + "</body></html>";
        return !management ? UiLanguage.localizeHtml(html, selectedLanguage) : html;
    }

    /** Header language selector: works without inline JavaScript (CSP script-src self). */
    private static String languagePicker(String code, boolean mobile) {
        StringBuilder b = new StringBuilder("<form method=\"get\" action=\"/language\" class=\"header-language ");
        b.append(mobile ? "header-language-mobile" : "header-language-desktop");
        b.append("\" aria-label=\"Chọn ngôn ngữ / Select language\">");
        b.append("<label class=\"language-select-label\">").append(icon("globe"));
        b.append("<select name=\"lang\" class=\"header-language-select\" aria-label=\"Ngôn ngữ / Language\">");
        String[][] choices = {{"vi", "Tiếng Việt"}, {"en", "English"}, {"zh", "中文"}, {"ko", "한국어"}};
        for (String[] choice : choices) {
            b.append("<option value=\"").append(choice[0]).append("\"");
            if (choice[0].equals(code)) b.append(" selected");
            b.append(">").append(choice[1]).append("</option>");
        }
        b.append("</select></label>");
        b.append("<input type=\"hidden\" name=\"next\" value=\"/\">");
        b.append("<button type=\"submit\" class=\"language-apply\">OK</button></form>");
        return b.toString();
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
