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


### Cập nhật phát audio (chỉ Client)
- Demo: chọn POI rồi nhấn **Phát thuyết minh**; khi chưa có audio mẫu, chọn file MP3/WAV có sẵn trên máy. Đây là file thử cục bộ, không phải audio từ MySQL.
- API thật: chọn **Microservices Java (API thật)**, khởi động narration-server và audio-server, đăng nhập quản trị và upload MP3 cho nội dung. Narration phải trả `audioUrl` hợp lệ. Client tải MP3 rồi mở bằng trình phát mặc định của Windows; WAV có thể phát trong Swing.
- Nếu narration-server trả `/pending.mp3`, đó là URL chưa hợp lệ do Backend lưu placeholder. Client không thể suy ra audio ID; cần Backend cập nhật URL thật trong DB. Không sửa Backend trong gói này.
- Nút dừng chỉ điều khiển WAV phát trong Swing; MP3 mở bằng trình phát hệ điều hành.
