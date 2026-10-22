# Hợp đồng API: Người dùng ẩn danh (Client ↔ Backend của nhóm)

**Phạm vi thay đổi:** chỉ `src/Client`. Đây là hợp đồng để bạn làm Backend độc lập triển khai `visitor-service`; không chỉnh sửa `auth-service`, `poi-service`, `narration-server` hay `audio-server` hiện tại.

## 1. Giao diện Client đã triển khai

- Một phiên ẩn danh **theo mỗi tab trình duyệt** (`sessionStorage` chứa UUID). Không cần đăng nhập.
- Client gửi tín hiệu ban đầu ngay khi tải trang và **mỗi 10 giây** tới `POST /client/visitor/activity` trên chính Client (mặc định cổng 8080).
- Sự kiện `play` khi HTML audio thực sự phát, `pause` khi tạm dừng, `ended` khi hết bài. Mở trang khác vẫn giữ UUID cùng tab. `close` gửi qua `sendBeacon` khi đóng hoặc rời trang.
- Trang quản trị: `/admin/users`; Dashboard `/admin` có hai số riêng **Đang truy cập** và **Đang nghe**; JavaScript tự làm mới mỗi **5 giây** bằng `GET /client/admin/visitor-summary` (chỉ admin đã xác thực).
- **Chế độ Demo:** Client giữ các phiên ẩn danh **trong RAM** để thử nghiệm, không lưu database, không thu thập IP/họ tên. Phiên hết hạn khi quá **30 giây** không có heartbeat. Trên UI, `—` nghĩa là chưa kết nối API (không tự đổi thành `0`).
- **Chế độ Live:** Client Java chuyển tiếp sang `visitor-service`, không tự dựng số đếm giả. Nếu Backend không hoạt động, UI báo "Chưa kết nối API người dùng".

## 2. Hai endpoint Backend cần triển khai (trong tương lai)

Cấu hình tại `Client/config.properties` hoặc `Client/local.properties`:

```properties
visitor.url=http://127.0.0.1:8086
```

### A. POST `/api/visitor/activity` (không yêu cầu đăng nhập)

Client Java sẽ POST JSON dạng:

```json
{
  "sessionId": "f27b3f4d-0cca-43ce-974d-3b945ed6bade",
  "event": "heartbeat",
  "page": "/place",
  "poiId": 1,
  "audioId": 4,
  "listening": false
}
```

`event`: `heartbeat`, `play`, `pause`, `ended`, `close`.

Backend cần: lưu phiên theo `sessionId`; cập nhật `lastSeen` theo heartbeat/sự kiện; chuyển **đang nghe** khi `play`, trở lại **đang truy cập** khi `pause/ended`; xóa hoặc đóng phiên khi `close`; **không tính phiên không có tín hiệu trên 30 giây**. Bảo vệ endpoint khỏi lạm dụng/đếm ảo (ví dụ rate limit). Có thể trả `200` hoặc `204` thành công.

### B. GET `/api/admin/visitors/online` (chỉ admin)

Client Java chuyển nguyên cookie phiên `SGU_ADMIN_SESSION` từ auth-service để Backend kiểm tra quyền quản trị (hoặc qua cơ chế auth chung do nhóm thống nhất). JSON phản hồi:

```json
{
  "connected": true,
  "online": 2,
  "listening": 1,
  "visitors": [
    {"visitorId":"f27b3f4d", "status":"listening", "poiId":1, "audioId":4, "page":"/place", "secondsAgo":8},
    {"visitorId":"874ecc20", "status":"browsing", "poiId":0, "audioId":0, "page":"/places", "secondsAgo":2}
  ]
}
```

`online` là **tổng phiên ẩn danh đang hoạt động**, bao gồm các phiên đang nghe; `listening` là **tập con** của `online`. Hai số không được cộng lại. Trả `401`/`403` nếu chưa được xác thực; Client sẽ không hiển thị số giả nếu server trả lỗi.

**Quyền riêng tư:** Chỉ dùng ID phiên ngẫu nhiên; không gửi email, tên khách, mật khẩu, IP, GPS. Không có tính năng tài khoản khách trong Client này.

## 3. Kiểm thử trước khi ghép Backend

Mở hai cửa sổ Chrome/Edge: một cửa sổ trang khách, cửa sổ kia đăng nhập Admin tại `/admin/users`. Demo sẽ tăng "Đang truy cập" cho phiên khách. Trên trang chi tiết POI, phát MP3 để thấy "Đang nghe" tăng, bấm tạm dừng để số này giảm, và đóng tab khách để phiên biến mất (hoặc tự hết hạn sau 30 giây nếu ngắt mạng). Cập nhật UI tự động mỗi 5 giây.

**Điểm khác:** `Client` viết Java 21 (HTML/CSS trình bày). Hai file JavaScript nhỏ trong `app/src/main/resources/assets/` chỉ phục vụ heartbeat, bắt sự kiện trình phát và tự cập nhật số liệu; nghiệp vụ Java và backend khác vẫn tách rời.
