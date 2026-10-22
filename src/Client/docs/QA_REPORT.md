# Kiểm tra bản bàn giao - 22/10/2026

## Đã chạy

- JDK 21 `javac --release 21`: client-core, client-gateway, narration-server, audio-server, client-server biên dịch thành công; tạo JAR chạy được.
- Bốn tiến trình Java HTTP thật khởi động trong chế độ DEMO. Web test dùng 8090 vì 8080 của môi trường kiểm tra đang bận; gói giao mặc định 8080.
- `tests/ClientTests.java`: **66 kiểm tra đạt**, chi tiết trong `java-tests.log`.
- JSON, Unicode, escape HTML; chống CSRF, rotation/revoke phiên; đăng nhập sai/đúng; POI thêm/trùng/sửa/ẩn; nội dung đúng POI-ngôn ngữ; upload MP3 thật và từ chối file giả; metadata, version; stream đúng bytes qua client -> gateway -> audio; HEAD/206/416; chặn audio của POI ẩn; preview quản trị.
- Biên dịch source auth-service (8 file) và poi-service (12 file, gồm 4 file mới) bằng Java 21 với dependency từ JAR gốc. Cả hai Spring server khởi động được; API quản trị trả 401 không có phiên. **Chưa truy vấn MySQL**.
- Parse XML của các `pom.xml` thành công. Không gọi đây là Maven build: môi trường không có Maven/tải Internet hoạt động.

## Kiểm tra hình ảnh / audio trình duyệt

Chromium render HTML do **Java server thực tế trả về**, kích thước 390px và 1440px, 8 trang/kích thước không bị tràn ngang. Kiểm tra desktop home/audio/admin/content, mobile home/audio/map/admin.
Môi trường Chromium chặn navigation đến localhost bằng chính sách quản trị. Không thay đổi chính sách: test lấy HTML từ HTTP Java, nhúng CSS/SVG/MP3 tương ứng và render snapshot bằng Playwright.
Đây là ảnh render giao diện đã viết, **không phải mockup concept**, nhưng không thay thế test navigation/network trên trình duyệt thiết bị thật.
Chromium nhận MP3 14,67 giây, readyState=4, lệnh Play làm currentTime tăng; không có media error. HTTP stream/Range qua các server đã được test riêng.
Kết quả chi tiết: `visual-tests.json`. Thư mục `previews/` có ảnh.

## Chưa kiểm tra, không được suy ra đã đạt

- MySQL thật với username/password/database của nhóm, tích hợp LIVE end-to-end.
- Safari/iPhone/iPad, Chrome Android trên thiết bị thật; kết nối Wi-Fi/Firewall ở nhà bạn.
- Chạy script PowerShell/Windows thật. Script được soát mã; build.sh/start-demo.sh/test.sh được chạy trên Linux.
- Bản đồ OSM qua Internet, ảnh POI đường dẫn bên ngoài; native APK/IPA/EXE; chịu tải production.

## Tự test lại

Chạy DEMO, rồi `test.bat`. Nếu đổi web port:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test.ps1 -BaseUrl http://localhost:8090
```

Test tạo bản ghi QA demo. Bản ZIP không kèm dữ liệu QA đã tạo trong lần kiểm tra. Lần mở đầu tự khởi tạo dữ liệu minh họa.
