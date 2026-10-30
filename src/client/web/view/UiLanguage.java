package web.view;

import core.Http;
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

/** Public-site languages are independent from backend content availability. */
public final class UiLanguage {
    private UiLanguage() {}

    public static boolean supported(String value) {
        if (value == null) return false;
        return Set.of("vi", "vi-vn", "en", "en-us", "en-gb", "zh", "zh-cn", "zh-tw", "cn", "ko", "ko-kr", "kr")
                .contains(value.toLowerCase(Locale.ROOT));
    }

    public static String normalize(String value) {
        if (!supported(value)) return "vi";
        String code = value.toLowerCase(Locale.ROOT);
        if (code.startsWith("en")) return "en";
        if (code.startsWith("zh") || code.equals("cn")) return "zh";
        if (code.startsWith("ko") || code.equals("kr")) return "ko";
        return "vi";
    }

    /** Explicit detail-page ?lang= choice takes precedence over the saved cookie. */
    public static String selected(HttpExchange x) {
        String requested = Http.query(x).get("lang");
        if (supported(requested)) return normalize(requested);
        String cookie = Http.cookie(x,"SGU_LANG");
        return supported(cookie) ? normalize(cookie) : "vi";
    }

    /** Allow only relative same-origin redirects, with the new detail-page narration language. */
    public static String returnPath(String raw, String lang) {
        if (raw == null || raw.length() > 2048 || !raw.startsWith("/") || raw.startsWith("//")
                || raw.indexOf('\\') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0)
            return "/";
        try {
            URI u = URI.create(raw);
            if (u.isAbsolute() || u.getRawAuthority() != null || u.getRawFragment() != null)
                return "/";
            if (!"/place".equals(u.getPath())) return raw;
            Map<String,String> q = new LinkedHashMap<>(Http.params(u.getRawQuery()));
            q.put("lang", normalize(lang));
            StringJoiner params=new StringJoiner("&");
            q.forEach((k,v)->params.add(URLEncoder.encode(k,StandardCharsets.UTF_8)
                    +"="+URLEncoder.encode(v,StandardCharsets.UTF_8)));
            return "/place?"+params;
        } catch (RuntimeException ex) {
            return "/";
        }
    }

    /** Only translate known, fixed UI text nodes. POI titles/descriptions are backend data. */
    public static String localizeHtml(String html, String code) {
        String lang=normalize(code);
        if ("vi".equals(lang)) return html;
        Map<String,String> translations=switch(lang) {
            case "en" -> EN;
            case "zh" -> ZH;
            case "ko" -> KO;
            default -> Map.of();
        };
        String output=html;
        for (var item:translations.entrySet()) {
            String source=item.getKey(), dest=item.getValue();
            // Translate only complete text nodes and input placeholders / accessible labels.
            output=output.replace(">"+source+"<", ">"+dest+"<")
                    .replace("placeholder=\""+source+"\"", "placeholder=\""+dest+"\"")
                    .replace("aria-label=\""+source+"\"", "aria-label=\""+dest+"\"");
        }
        String count=switch(lang) {
            case "en" -> " places shown";
            case "zh" -> " 个地点正在显示";
            case "ko" -> "개 장소 표시 중";
            default -> " địa điểm đang hiển thị";
        };
        output=output.replace(" địa điểm đang hiển thị</div>",count+"</div>");
        // Static text adjacent to an icon is not a standalone HTML text node.
        String[][] tails=switch(lang) {
            case "en" -> new String[][]{{"Bắt đầu khám phá ","Start exploring "},
                    {"Tất cả địa điểm ","All places "},{"Trải nghiệm mô phỏng ","Try the map "},
                    {"Xem bản đồ","View map"},{"Xem trên bản đồ","View on map"},
                    {"Xem vị trí & mô phỏng GPS","View location & GPS simulation"},
                    {"Nghe thuyết minh","Listen to audio guide"},
                    {"Khám phá câu chuyện","Explore the story"}};
            case "zh" -> new String[][]{{"Bắt đầu khám phá ","开始探索 "},
                    {"Tất cả địa điểm ","全部地点 "},{"Trải nghiệm mô phỏng ","体验地图 "},
                    {"Xem bản đồ","查看地图"},{"Xem trên bản đồ","在地图上查看"},
                    {"Xem vị trí & mô phỏng GPS","查看位置与 GPS 模拟"},
                    {"Nghe thuyết minh","收听语音导览"},
                    {"Khám phá câu chuyện","探索故事"}};
            case "ko" -> new String[][]{{"Bắt đầu khám phá ","탐험 시작 "},
                    {"Tất cả địa điểm ","모든 장소 "},{"Trải nghiệm mô phỏng ","지도 체험 "},
                    {"Xem bản đồ","지도 보기"},{"Xem trên bản đồ","지도에서 보기"},
                    {"Xem vị trí & mô phỏng GPS","위치 및 GPS 시뮬레이션 보기"},
                    {"Nghe thuyết minh","오디오 가이드 듣기"},
                    {"Khám phá câu chuyện","이야기 살펴보기"}};
            default -> new String[0][0];
        };
        for(String[] t:tails) {
            // Avoid changing user-generated POI data in <p> and <h3>.
            output=output.replace("</svg>"+t[0],"</svg>"+t[1]);
            output=output.replace("</span>"+t[0],"</span>"+t[1]);
            output=output.replace("</a>"+t[0],"</a>"+t[1]);
        }
        return output;
    }

    private static Map<String,String> texts(String... pairs) {
        Map<String,String> value=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)value.put(pairs[i],pairs[i+1]);
        return Collections.unmodifiableMap(value);
    }
    private static final Map<String,String> EN=texts(
        "Trường Đại học Sài Gòn","Saigon University",
        "Khám phá","Explore", "Địa điểm","Places", "Bản đồ","Map",
        "Đến nội dung","Skip to content",
        "Một khuôn viên.","One campus.", "Muôn câu chuyện.","Countless stories.",
        "Khám phá Trường Đại học Sài Gòn theo cách của bạn. Chọn địa điểm, lắng nghe thuyết minh và bắt đầu một hành trình mới.",
        "Explore Saigon University your way. Choose a place, listen to its audio guide, and begin a new journey.",
        "Điểm dừng chân tiếp theo","Your next stop",
        "Mỗi địa điểm là một câu chuyện đang chờ bạn.","Every place has a story waiting for you.",
        "Chưa đến trường? Vẫn có thể khám phá.","Not on campus? You can still explore.",
        "Chọn một POI trên bản đồ để mô phỏng vị trí và nghe thuyết minh.","Choose a POI on the map to simulate your location and listen to its guide.",
        "Địa điểm tham quan","Places to visit", 
        "Tìm tên địa điểm, mã POI hoặc khu vực...","Search by place, POI code or area...",
        "Tìm kiếm","Search", "Chưa có địa điểm phù hợp","No matching places",
        "Thử từ khóa khác hoặc thêm POI trong trang quản trị.","Try another keyword or ask an administrator to add a POI.",
        "Minh họa địa điểm","Illustrative image", "Giới thiệu","Introduction",
        "Nghe thuyết minh","Listen to audio guide", "Câu chuyện được kể theo cách của bạn","Stories told your way",
        "CHỌN NGÔN NGỮ","CHOOSE LANGUAGE",
        "Chưa có bài thuyết minh","No audio guide yet", "Chưa có nội dung bằng ngôn ngữ này. Hãy chọn ngôn ngữ khác.","No content is available in this language. Try another language.",
        "Nội dung này chưa được gắn MP3. Quản trị viên có thể tải file trong Nội dung & audio.","No MP3 is linked to this narration yet.",
        "Bấm nút phát để nghe. Có thể tạm dừng và kéo thanh thời gian ngay trong ứng dụng.","Press play to listen. You can pause or seek within the audio.",
        "Trình duyệt của bạn chưa hỗ trợ phát audio.","Your browser does not support audio playback.",
        "Bản đồ khám phá","Explore the map",
        "Sơ đồ tọa độ","Coordinate map", "Bản đồ OpenStreetMap","OpenStreetMap",
        "Sơ đồ tọa độ, không phải bản đồ đường đi","Coordinate diagram, not a route map",
        "Bản đồ nền OpenStreetMap cần Internet. Sơ đồ tọa độ vẫn hoạt động khi không tải được bản đồ nền.","OpenStreetMap needs an internet connection. The coordinate map works offline.",
        "Mô phỏng GPS bằng tọa độ","Simulate GPS with coordinates",
        "Vĩ độ","Latitude","Kinh độ","Longitude",
        "Chọn điểm dừng","Choose a stop", "Tìm POI phù hợp","Find nearby POI",
        "Địa điểm được chọn ở chế độ mô phỏng. Không yêu cầu truy cập vị trí của bạn.","This is a simulated location. Your real location is not requested.",
        "Chưa có POI trên bản đồ","No POIs on the map",
        "Chọn ngôn ngữ / Select language","Choose language", "Ngôn ngữ / Language","Language"
    );
    private static final Map<String,String> ZH=texts(
        "Trường Đại học Sài Gòn","西贡大学",
        "Khám phá","探索", "Địa điểm","地点", "Bản đồ","地图", "Đến nội dung","跳转到内容",
        "Một khuôn viên.","一个校园。", "Muôn câu chuyện.","万千故事。",
        "Khám phá Trường Đại học Sài Gòn theo cách của bạn. Chọn địa điểm, lắng nghe thuyết minh và bắt đầu một hành trình mới.",
        "以自己的方式探索西贡大学。选择地点、聆听语音导览，开启新的旅程。",
        "Điểm dừng chân tiếp theo","下一站",
        "Mỗi địa điểm là một câu chuyện đang chờ bạn.","每个地点都有一个故事等待你发现。",
        "Chưa đến trường? Vẫn có thể khám phá.","不在校园？也能在线探索。",
        "Chọn một POI trên bản đồ để mô phỏng vị trí và nghe thuyết minh.","在地图上选择景点，模拟位置并收听语音导览。",
        "Địa điểm tham quan","参观地点",
        "Tìm tên địa điểm, mã POI hoặc khu vực...","搜索地点名称、POI 编码或区域...",
        "Tìm kiếm","搜索","Chưa có địa điểm phù hợp","没有匹配的地点",
        "Thử từ khóa khác hoặc thêm POI trong trang quản trị.","请尝试其他关键词或请管理员添加地点。",
        "Minh họa địa điểm","地点示意图","Giới thiệu","介绍",
        "Nghe thuyết minh","收听语音导览","Câu chuyện được kể theo cách của bạn","用你喜欢的方式聆听故事",
        "CHỌN NGÔN NGỮ","选择语言",
        "Chưa có bài thuyết minh","尚无语音导览","Chưa có nội dung bằng ngôn ngữ này. Hãy chọn ngôn ngữ khác.","此语言暂无内容，请选择其他语言。",
        "Nội dung này chưa được gắn MP3. Quản trị viên có thể tải file trong Nội dung & audio.","此讲解尚未关联 MP3 音频。",
        "Bấm nút phát để nghe. Có thể tạm dừng và kéo thanh thời gian ngay trong ứng dụng.","点击播放以收听，也可暂停或拖动进度条。",
        "Trình duyệt của bạn chưa hỗ trợ phát audio.","您的浏览器不支持音频播放。",
        "Bản đồ khám phá","探索地图",
        "Sơ đồ tọa độ","坐标图", "Bản đồ OpenStreetMap","OpenStreetMap 地图",
        "Sơ đồ tọa độ, không phải bản đồ đường đi","坐标示意图，非导航路线",
        "Bản đồ nền OpenStreetMap cần Internet. Sơ đồ tọa độ vẫn hoạt động khi không tải được bản đồ nền.","OpenStreetMap 需要联网，离线时仍可使用坐标图。",
        "Mô phỏng GPS bằng tọa độ","通过坐标模拟 GPS",
        "Vĩ độ","纬度","Kinh độ","经度", "Chọn điểm dừng","选择站点", "Tìm POI phù hợp","查找附近地点",
        "Địa điểm được chọn ở chế độ mô phỏng. Không yêu cầu truy cập vị trí của bạn.","当前使用模拟位置，不会请求您的实际位置。",
        "Chưa có POI trên bản đồ","地图上暂无地点",
        "Chọn ngôn ngữ / Select language","选择语言", "Ngôn ngữ / Language","语言"
    );
    private static final Map<String,String> KO=texts(
        "Trường Đại học Sài Gòn","사이공 대학교",
        "Khám phá","탐험", "Địa điểm","장소", "Bản đồ","지도", "Đến nội dung","본문으로 이동",
        "Một khuôn viên.","하나의 캠퍼스.", "Muôn câu chuyện.","수많은 이야기.",
        "Khám phá Trường Đại học Sài Gòn theo cách của bạn. Chọn địa điểm, lắng nghe thuyết minh và bắt đầu một hành trình mới.",
        "사이공 대학교를 나만의 방식으로 탐험해 보세요. 장소를 선택하고 오디오 가이드를 들으며 새로운 여정을 시작하세요.",
        "Điểm dừng chân tiếp theo","다음 방문지",
        "Mỗi địa điểm là một câu chuyện đang chờ bạn.","모든 장소에는 새로운 이야기가 기다리고 있습니다.",
        "Chưa đến trường? Vẫn có thể khám phá.","학교에 오지 않아도 탐험할 수 있어요.",
        "Chọn một POI trên bản đồ để mô phỏng vị trí và nghe thuyết minh.","지도에서 장소를 선택해 위치를 시뮬레이션하고 안내를 들어보세요.",
        "Địa điểm tham quan","방문 장소", 
        "Tìm tên địa điểm, mã POI hoặc khu vực...","장소 이름, POI 코드 또는 구역 검색...",
        "Tìm kiếm","검색","Chưa có địa điểm phù hợp","일치하는 장소가 없습니다",
        "Thử từ khóa khác hoặc thêm POI trong trang quản trị.","다른 검색어를 사용하거나 관리자에게 장소 추가를 요청하세요.",
        "Minh họa địa điểm","장소 예시 이미지","Giới thiệu","소개",
        "Nghe thuyết minh","오디오 가이드 듣기","Câu chuyện được kể theo cách của bạn","원하는 방식으로 듣는 이야기",
        "CHỌN NGÔN NGỮ","언어 선택",
        "Chưa có bài thuyết minh","아직 오디오 가이드가 없습니다","Chưa có nội dung bằng ngôn ngữ này. Hãy chọn ngôn ngữ khác.","이 언어로 제공되는 콘텐츠가 없습니다. 다른 언어를 선택하세요.",
        "Nội dung này chưa được gắn MP3. Quản trị viên có thể tải file trong Nội dung & audio.","이 설명에는 아직 MP3 파일이 연결되지 않았습니다.",
        "Bấm nút phát để nghe. Có thể tạm dừng và kéo thanh thời gian ngay trong ứng dụng.","재생 버튼을 눌러 들어보세요. 일시정지하거나 재생 위치를 이동할 수 있습니다.",
        "Trình duyệt của bạn chưa hỗ trợ phát audio.","브라우저가 오디오 재생을 지원하지 않습니다.",
        "Bản đồ khám phá","지도 탐색",
        "Sơ đồ tọa độ","좌표 지도", "Bản đồ OpenStreetMap","OpenStreetMap 지도",
        "Sơ đồ tọa độ, không phải bản đồ đường đi","좌표 도식(경로 안내 아님)",
        "Bản đồ nền OpenStreetMap cần Internet. Sơ đồ tọa độ vẫn hoạt động khi không tải được bản đồ nền.","OpenStreetMap에는 인터넷이 필요하며, 좌표 지도는 오프라인에서도 작동합니다.",
        "Mô phỏng GPS bằng tọa độ","좌표로 GPS 시뮬레이션",
        "Vĩ độ","위도","Kinh độ","경도", "Chọn điểm dừng","방문지 선택", "Tìm POI phù hợp","근처 장소 찾기",
        "Địa điểm được chọn ở chế độ mô phỏng. Không yêu cầu truy cập vị trí của bạn.","현재 위치는 시뮬레이션이며 실제 위치를 요청하지 않습니다.",
        "Chưa có POI trên bản đồ","지도에 장소가 없습니다",
        "Chọn ngôn ngữ / Select language","언어 선택", "Ngôn ngữ / Language","언어"
    );
}
