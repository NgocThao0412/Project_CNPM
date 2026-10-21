# CLIENT JAVA — SGU AUDIO GUIDE

Ứng dụng **Client** được xây dựng bằng **Java 21 Swing**, không còn HTML/CSS/JavaScript/Node.js. Đây là ứng dụng **desktop Java** kết nối với backend Spring Boot qua HTTP REST (Microservices / API Gateway).

> **Quan trọng:** Client Java **không tự làm biến backend thành Microservices**. Repo gốc hiện vẫn là một Spring Boot application, chỉ có `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`, `GET /api/pois` và health checks. Bạn Backend cần tách/tạo các Java service còn thiếu và hoàn thiện API trước khi demo dữ liệu thật. `config.properties` giữ tất cả các feature chưa tồn tại ở giá trị `false`. **Không tuyên bố thao tác thành công khi API chưa có.**

## 1. Các giao diện Java đã làm

| Chức năng Client | Java Swing |
| --- | --- |
| Form đăng nhập, thông báo lỗi, đăng xuất, khung quản trị | `ClientApp.java`, `HttpApi.java` (JSESSIONID) |
| Quản lý POI: danh sách, tìm kiếm, thêm/sửa, ẩn hiện | `ClientApp.java`, `Models.java` |
| Quản lý nội dung: POI, ngôn ngữ, biểu mẫu; tải audio | `ClientApp.java`, `HttpApi.java` |
| Trang khách: bản đồ, POI mô phỏng, GPS theo tọa độ, chọn ngôn ngữ | `MapPanel.java`, `ClientApp.java` |
| Nghe audio | `AudioPlayer.java` |
| Lớp giao tiếp với Java Microservices | `ClientApi.java`, `HttpApi.java`, `ClientConfig.java` |

Bản đồ lấy ảnh tile từ **OpenStreetMap** bằng Java HttpClient. Nếu không có Internet, sẽ vẫn hiện lưới tọa độ và marker POI (không có nền đường phố). Tọa độ GPS là do người dùng nhập hoặc chọn POI **mô phỏng**, không tự động lấy GPS từ thiết bị. Audio WAV phát trực tiếp qua Java Sound, **MP3 mở qua ứng dụng phát mặc định của hệ điều hành**, không có nút tạm dừng MP3 bên trong Swing.

## 2. Yêu cầu

- JDK **21** trở lên (`java -version` và `javac -version`).
- Windows, Linux hoặc macOS có giao diện đồ họa (Swing).
- Không cần cài Node.js, npm, Maven, JavaFX hoặc thư viện Java bên ngoài để chạy **Client**.
- Nếu chạy chế độ Microservices thật: có Backend Spring Boot, database MySQL, API phục vụ dữ liệu.

## 3. Chạy trên Windows

Giải nén ZIP rồi vào thư mục `Client`, bấm `run.bat` hoặc mở terminal:

```powershell
cd Client
.\run.bat
```

Có thể mở file `Client.jar` bằng terminal:

```powershell
java -jar Client.jar
```

Lưu ý: chạy lệnh tại thư mục có `config.properties` (nếu chạy JAR từ nơi khác, dùng `java -jar ...` và đặt `config.properties` trong thư mục hiện hành).

Trên Linux/macOS: `./run.sh`.

## 4. Demo / Real Backend

Chọn chế độ từ dropdown ở góc phải:

- **Demo:** tài khoản `admin@sgu.edu.vn` hoặc `admin`, mật khẩu **`123456`**. Dữ liệu POI và nội dung chỉ nằm trong bộ nhớ, mất khi đóng chương trình. Không phải đăng nhập thật và không ghi MySQL.
- **Microservices Java:** gọi API thật. Cần tài khoản `username` hợp lệ trong Backend Java. Mật khẩu thật là giá trị trong MySQL; **không mặc định 123456**.

`HttpApi` gửi `POST /api/auth/login` với `{ "username": "...", "password": "..." }`, nhận `JSESSIONID`, và giữ cookie trong `CookieManager` cho `/api/auth/me` và các request sau.

## 5. Cấu hình giao tiếp Microservices

Sửa `config.properties`:

```properties
service.gateway=http://localhost:8080
# Sau này nếu đã tách thật:
# service.auth=http://localhost:8081
# service.poi=http://localhost:8082
# service.narration=http://localhost:8083
# service.audio=http://localhost:8084

feature.poi.write=false
feature.narration.read=false
feature.narration.write=false
feature.audio.write=false
feature.visitor.events=false
```

**Endpoint sẵn có của backend ZIP đã tải lên** (Java Spring Boot):

| HTTP | Endpoint | Trạng thái Backend |
| --- | --- | --- |
| POST | `/api/auth/login` | Có |
| GET | `/api/auth/me` | Có |
| POST | `/api/auth/logout` | Có |
| GET | `/api/pois` | Có, chỉ trả POI đang hiển thị |

**Contract đề xuất cho bạn Backend bổ sung** (chưa được backend hiện có triển khai):

| HTTP | Endpoint | Java Client sử dụng |
| --- | --- | --- |
| POST / PUT | `/api/pois` / `/api/pois/{id}` | Thêm và sửa POI |
| PATCH | `/api/pois/{id}/visibility` | Ẩn/hiện POI |
| GET | `/api/narrations` | Danh sách nội dung |
| POST / PUT | `/api/narrations` / `/api/narrations/{id}` | Lưu nội dung |
| PATCH | `/api/narrations/{id}/visibility` | Ẩn/hiện nội dung |
| POST multipart | `/api/audio/narrations/{id}` | Tải MP3/WAV trường `audio` |
| POST | `/api/listening-logs` | Ghi nhận nghe |

Trường POI Java DTO: `id`, `poiCode`, `poiName`, `areaId`, `areaName`, `description`, `latitude`, `longitude`, `triggerRadiusM`, `imageUrl`. Dữ liệu JSON dùng camelCase (theo `POIDTO` trong repo). Narration DTO sử dụng `id`, `poiId`, `languageCode`, `title`, `content`, `audioUrl`, `isVisible`. Đây là **đề xuất** cần thống nhất lại với backend.

Nếu API chưa tồn tại, các thao tác tương ứng hiển thị thông báo thay vì ghi dữ liệu demo. Khi API thực sự triển khai xong, bật cờ tính năng trong `config.properties`, kiểm thử với Database thật.

**Lưu ý bảo mật:** backend gốc tạo `HttpSession` trong Spring Boot. Khi tách thành nhiều service, phiên này không tự chia sẻ. Để chạy microservices thật, cần gateway, cookie session chung/centralized session store hoặc truyền xác thực nội bộ phù hợp. Đừng chỉ mở từng cổng và giả định các service đã đồng bộ xác thực.

## 6. Kiểm thử

Trên Windows chạy `test.bat`, trên Linux/macOS chạy `./test.sh`.

Bộ kiểm thử chạy bằng Java JDK, có mock HTTP server và kiểm tra các yêu cầu, bao gồm xử lý JSON, demo POI/narration, POIDTO, phiên `JSESSIONID`, API Login / Me / Logout, và xử lý những endpoint backend chưa triển khai.

## 7. Cấu trúc

```
Client/
├── Client.jar                     # JAR có sẵn, mở Java GUI
├── config.properties
├── run.bat / run.sh                 # Biên dịch và chạy từ source
├── test.bat / test.sh               # Test bằng JDK 21
├── src/com/sgu/client/
│   ├── ClientApp.java            # Swing UI: Login, Admin, Visitor
│   ├── HttpApi.java                # Java REST adapter, cookie/session
│   ├── DemoApi.java                # Chỉ chạy demo
│   ├── ClientApi.java              # Interface API
│   ├── ClientConfig.java           # URL các Microservices
│   ├── Models.java                 # DTO mapping và validation
│   ├── Json.java                   # JSON codec Java
│   ├── MapPanel.java               # Bản đồ OSM Java
│   └── AudioPlayer.java            # WAV Java Sound / MP3 OS player
└── tests/ClientTests.java
```

## 8. Quy trình demo đề xuất

1. Chạy Java Backend, MySQL; gọi GET `/api/health` và GET `/api/pois`.
2. Chạy `Client.jar`, chọn `Microservices Java`.
3. Xem POI trên bản đồ; mở Login và đăng nhập bằng username backend thật.
4. Thử chức năng POI/narration chỉ sau khi bạn Backend hoàn thiện và bật các flags trong `config.properties`.
5. Test cập nhật MySQL và chơi audio có URL thật.

Không push `.class`/`build` lên GitHub; `Client.jar` có thể giữ trong bản bàn giao, nhưng với source repo nên chỉ push `.java`, `config.properties`, script và README.
