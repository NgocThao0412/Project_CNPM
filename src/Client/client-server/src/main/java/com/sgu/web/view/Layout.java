
package com.sgu.web.view;

import com.sgu.core.*;
import com.sgu.web.DTO.*;

import static com.sgu.web.view.Html.*;

public final class Layout {

    private final Settings s;

    public Layout(Settings s) {
        this.s = s;
    }

    public String page(
            ClientSessionDTO ss,
            String title,
            String tab,
            String content,
            String notice) {

        return "<!doctype html>"
                + "<html lang=\"vi\">"
                + "<head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<meta name=\"theme-color\" content=\"#1262cb\">"
                + "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">"
                + "<title>" + e(title) + " | SGU Audio Guide</title>"
                + "<link rel=\"icon\" href=\"/assets/mark.svg\" type=\"image/svg+xml\">"
                + "<link rel=\"stylesheet\" href=\"/assets/app.css\">"
                + "</head>"

                + "<body>"

              
                + "<a class=\"skip\" href=\"#main\">Đến nội dung</a>"

                
                + "<header class=\"site-header\">"
                + "<div class=\"header-inner\">"

               
                + link(
                         "/",
                         "<img src=\"/assets/sgu-logo.png\" "
                         + "alt=\"Logo Trường Đại học Sài Gòn\" "
                         + "class=\"sgu-logo\">"
                         + "<span class=\"brand-text\">"
                         + "<strong>SGU <b>Audio Guide</b></strong>"
                         + "<small>EXPLORE · LISTEN · DISCOVER</small>"
                         + "</span>",
                         "brand"
                )

             
                + "<nav class=\"desktop-nav\">"
                + nav("/", "Khám phá", "home", tab)
                + nav("/places", "Địa điểm", "places", tab)
                + nav("/map", "Bản đồ", "map", tab)
                + "</nav>"

                
                + "</div>"
                + "</header>"

               
                + "<main id=\"main\" class=\"page\">"
                + (notice == null || notice.isBlank()
                        ? ""
                        : "<div role=\"status\" class=\"notice\">"
                        + icon("check")
                        + e(notice)
                        + "</div>")
                + content
                + "</main>"


                + "<footer class=\"site-footer\">"
                + "<strong>SGU Audio Guide</strong>"
                + "<span>Đồ án Công nghệ phần mềm · Nhóm 12</span>"
                + "</footer>"

             
                + "<nav class=\"bottom-nav\" aria-label=\"Điều hướng mobile\">"
                + mobile("/", "Khám phá", "home", tab, "home")
                + mobile("/places", "Địa điểm", "places", tab, "pin")
                + mobile("/map", "Bản đồ", "map", tab, "map")

          
                + "</nav>"

                + "</body>"
                + "</html>";
    }

 
    private String nav(
            String href,
            String label,
            String key,
            String current) {

        return link(
                href,
                e(label),
                key.equals(current)
                        ? "nav-link active"
                        : "nav-link"
        );
    }

  
    private String mobile(
            String href,
            String label,
            String key,
            String current,
            String ic) {

        return link(
                href,
                icon(ic) + "<span>" + e(label) + "</span>",
                key.equals(current)
                        ? "active"
                        : ""
        );
    }

  
    public String adminNav(
            ClientSessionDTO ss,
            String active) {

        return "<div class=\"admin-header\">"
                + "<div>"
                + "<span class=\"eyebrow\">WORKSPACE</span>"
                + "<h1>Không gian quản trị</h1>"
                + "<p>Quản lý địa điểm và câu chuyện của SGU.</p>"
                + "</div>"

         
                + "<form method=\"post\" action=\"/logout\">"
                + hidden("csrf", ss.csrf)
                + "<button class=\"btn btn-light\">Đăng xuất</button>"
                + "</form>"
                + "</div>"

             
                + "<nav class=\"tabs\">"
                + link(
                        "/admin",
                        icon("home") + "Tổng quan",
                        active.equals("overview") ? "active" : ""
                )
                + link(
                        "/admin/pois",
                        icon("pin") + "Quản lý POI",
                        active.equals("pois") ? "active" : ""
                )
                + link(
                        "/admin/contents",
                        icon("book") + "Nội dung & audio",
                        active.equals("contents") ? "active" : ""
                )
                + "</nav>";
    }

  
    public String error(String message) {

        return "<div class=\"error-box\" role=\"alert\">"
                + "<strong>Chưa thể hoàn tất thao tác</strong>"
                + "<p>" + e(message) + "</p>"
                + "</div>";
    }
}
