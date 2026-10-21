# Client — Java Swing 21

Client Java desktop độc lập với các service Backend. Có cấu trúc `BUS/DAO/DTO/controller/client` và `pom.xml` riêng.

- Chế độ Demo: dữ liệu RAM, admin / 123456, không lưu MySQL.
- Chế độ Java Microservices: các API thật tại auth-server:8081, poi-server:8082, narration-server:8083, audio-server:8084.
- Mở `config.properties` để thay địa chỉ khi chạy trên máy khác.
- Biên dịch và chạy Windows: `run.bat`; Linux/macOS: `sh run.sh`. Hoặc `java -jar Client.jar`.
- Tự kiểm thử: `test.bat` hoặc `sh test.sh`.
- Không dùng `npm` / Node / HTML / React.
- Nhập username `admin`, mật khẩu do `auth-server` tạo (bootstrap dev dùng biến môi trường `SGU_ADMIN_PASSWORD`).
- Khi thêm POI, cần `areaId` thực từ MySQL; khi tải audio MP3 nhập thời lượng thực (giây).
- Client không truy cập MySQL trực tiếp, không tự xác thực cookie; mọi cập nhật thực sự đi qua service Java.

Xem hướng dẫn toàn bộ dự án ở `../README.md`.
