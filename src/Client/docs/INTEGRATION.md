# Tích hợp với Project_CNPM(2).zip

## Kết quả đối chiếu source

Nguồn: file `Project_CNPM(2).zip` do bạn gửi. Hai module thực tế là `src/Backend/auth-service`, `src/Backend/poi-service`; có SQL `Database/SGU_audio_guide.sql`.
Không có Client/narration/audio module, INSERT seed hay file MP3 trong ZIP này.

Auth gốc dùng username/password, HttpSession và cookie `SGU_ADMIN_SESSION`. Client không gửi email/Bearer token giả.
POI gốc có `GET /api/pois`, `POST /api/admin/pois`; chưa có admin list, PUT sửa, PATCH ẩn/hiện, GET khu vực.
Không thể làm giao diện sửa/ẩn lưu thật khi backend chưa có route tương ứng.

## File gốc được giữ nguyên

Bản ghép giữ nội dung tất cả file source/cấu hình/SQL gốc. Không đổi mật khẩu, không xóa bảng, không chỉnh class Auth của bạn trong nhóm.
Loại các thư mục build/cache `.git`, `.vs`, `target` khỏi bản ghép; JAR mới đặt riêng trong `tools/prebuilt-backend`.

Bốn file **mới, không ghi đè file cũ** được bổ sung vào poi-service:

```text
src/main/java/com/sgu/audioguide/
  DTO/POIManagementDTO.java
  DAO/POIManagementDAO.java
  BUS/POIManagementBUS.java
  controller/POIManagementController.java
```

Trong ZIP Client riêng, chúng nằm dưới `integration/poi-service-additions/`. Chép đúng vào poi-service rồi build; trong ZIP dự án ghép, đã được chép sẵn. Nếu bạn cùng nhóm đã thêm các endpoint này trong bản mới hơn, cần đối chiếu trước để tránh trùng route.

## Khởi tạo MySQL lần đầu

1. Sao lưu database nếu đã có dữ liệu. Dùng MySQL 8 theo schema gốc, không thay bằng SQLite/MariaDB để rồi khẳng định tương thích.
2. Chỉ import `Database/SGU_audio_guide.sql` khi chưa có bảng. Script gốc CREATE TABLE không có IF NOT EXISTS cho từng bảng; không chạy lại tùy tiện.
3. Chuẩn bị user MySQL `sgu_app` đúng nhóm cấu hình, có quyền cần thiết trên `SGU_audio_guide`. Hoặc đặt `SGU_DB_USERNAME` bằng user được nhóm cấp. Không đưa mật khẩu lên GitHub.
4. Nếu chưa có khu vực/ngôn ngữ, có thể chạy thủ công `integration/catalog-optional.sql`: chỉ thêm vi/en và một khu vực minh họa nếu chưa tồn tại, không cập nhật/xóa dữ liệu cũ.
5. Có tài khoản `admins` hợp lệ. Cơ chế bootstrap có sẵn trong source auth chỉ tạo admin nếu chưa tồn tại.

Tạo `local.properties` trong Client hoặc đặt biến môi trường. Ví dụ file cấu hình (thay giá trị giả):

```properties
sgu.db.url=jdbc:mysql://127.0.0.1:3306/SGU_audio_guide?connectionTimeZone=UTC
sgu.db.username=sgu_app
sgu.db.password=YOUR_MYSQL_PASSWORD
```

Khởi động LIVE bản ghép:

```powershell
.\start-live.bat
```

Trường hợp database hoàn toàn mới, chưa có admin, chỉ định bootstrap một cách tường minh:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start.ps1 -Mode live -BootstrapAdmin
```

Script hỏi mật khẩu MySQL và mật khẩu tài khoản admin mới, không lưu chúng vào file. Dừng server cũ trước; bootstrap không tác dụng nếu auth-service cũ đang chạy và được script tái sử dụng. Dùng `admin` / mật khẩu đã nhập để đăng nhập LIVE.

## Luồng dữ liệu thật

- Client form POI → Java controller/DAO → gateway → poi-service → `points_of_interest`.
- Chọn POI + ngôn ngữ, lưu thuyết minh → narration-server → `narration_contents`.
- Tải MP3 trong trang bài thuyết minh → audio-server → file UUID ở `data/live/audio` + metadata `audio_files`.
- Trang khách lấy đúng `poiId`/`languageId`, đọc `contentId`, tìm bản audio đang hiển thị, phát từ đường dẫn cùng website.
- Thay MP3: khóa dòng nội dung, ẩn bản cũ, tạo bản mới trong transaction. Không ghi đè file cũ. Không dùng `pending.mp3`.
- Không thay POI/ngôn ngữ của nội dung cũ, đúng constraint/trigger của SQL.
- File audio cũ ở bản dự án khác không tự di chuyển: cần tải lại qua trang quản trị. Không suy đoán URL hỏng thành URL đúng.

## Nghiệm thu LIVE trên máy nhóm

Mở `/status`; đăng nhập; thêm một POI và kiểm tra dòng MySQL; sửa tên; thêm bài vi/en; upload MP3 đúng ngôn ngữ; kiểm tra metadata và file; đăng xuất; mở trang khách trên laptop, iPhone, Android; phát/tạm dừng/tua; ẩn POI và xác nhận nội dung/audio không còn công khai. Ghi kết quả thật, không dùng kết quả DEMO thay thế.

## Biên dịch lại backend gốc

JAR ở `tools/prebuilt-backend` được biên dịch từ source Java 21, dùng lại Spring loader và dependency từ JAR người dùng đính kèm. Không khẳng định đã chạy Maven trong môi trường kiểm tra.
Sau khi sửa source, dùng wrapper gốc tại root Project_CNPM với Internet:

```powershell
.\mvnw.cmd -f .\src\Backend\auth-service\pom.xml clean package
.\mvnw.cmd -f .\src\Backend\poi-service\pom.xml clean package
```

Dùng JAR mới trong target để thay JAR tương ứng ở tools/prebuilt-backend; hoặc tự chạy service mới riêng trước start-live.
