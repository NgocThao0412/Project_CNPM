# Kết nối visitor-service cổng 8085 — chỉ sửa Client Java

**Nguồn hợp đồng:** 2 ảnh yêu cầu tích hợp do nhóm cung cấp. Không thêm/sửa bất kỳ file nào trong `src/backend`.

Client web ở `http://<IP-máy-chạy>:8080`. Điện thoại **chỉ** gửi HTTP về Client 8080; Java Client chuyển tiếp server-to-server đến `visitor-service` chạy `http://127.0.0.1:8085`. Cấu hình Client: `visitor.url` trong `config.properties` (hoặc `VISITOR_URL`/`local.properties`).

| Browser → Client :8080 | Client Java → Backend :8085 | Yêu cầu |
|---|---|---|
| `POST /api/visitor/sessions` | `POST /api/visitor/sessions` | Tạo phiên ẩn danh, JSON `{ "token": "..." }` |
| `POST /api/visitor/heartbeat` | `POST /api/visitor/heartbeat` | Header `X-Visitor-Token: <token>`, body JSON `{ "listening": true/false }` |
| `POST /api/visitor/end` | `POST /api/visitor/end` | Header `X-Visitor-Token: <token>`, kết thúc phiên |
| `GET /api/admin/visitor-stats` | `GET /api/admin/visitor-stats` | Java Client gửi cookie admin `SGU_ADMIN_SESSION`, JSON `{ "activeVisitors": 4, "activeListeners": 2 }` |

**Lưu ý hợp đồng:** Client không giả định Backend trả về danh sách chi tiết khách, địa chỉ IP hoặc danh tính. Nếu Backend trả `401/403/5xx`, GUI hiển thị `—` chứ không tự tạo số liệu.

## Vòng đời một phiên khách

1. Khách mở một tab website → JS gửi `POST /api/visitor/sessions` và giữ **token Backend cấp** trong `sessionStorage` của tab.
2. Khi khách chuyển giữa các trang nội bộ, Client **tái sử dụng token**. Nếu token hết hạn hoặc bị Backend từ chối (`401/404/410`), JS đăng ký phiên mới.
3. Gửi heartbeat khi mở trang và **10 giây/lần**. Dùng sự kiện **HTMLAudioElement `playing`** để gửi `listening:true` khi audio thực sự phát; `pause`, `ended`, `error`, `abort`, `emptied` → `false`. Không có audio thì `false`.
4. Khi đóng/rời trang, JS **cố gắng** POST `/end` bằng `fetch` có `keepalive:true` và header token. Điều hướng nội bộ cố ý không gửi `/end`. Nếu trình duyệt tắt đột ngột hoặc mạng mất, Backend loại phiên sau **30 giây** không nhận heartbeat.
5. Trang `/admin` và `/admin/users` lấy hai số riêng **mỗi 5 giây**. `activeListeners` là một tập con của `activeVisitors`; **không cộng** hai số.

Phía Browser chỉ gọi same-origin :8080, không gọi `http://localhost:8085` (localhost trên điện thoại là chính điện thoại). Chính Java Client đảm nhiệm header token và cookie admin khi gọi :8085.

## Điều kiện để chạy LIVE

Cần `auth-service` (8081), `poi-service` (8082), `narration-server` (8083), `audio-server` (8084) và Backend `visitor-service` (8085) đang sẵn sàng; cập nhật cấu hình cơ sở dữ liệu theo dự án tổng. Chạy Client LIVE trên Windows: 

```powershell
cd .\src\client
.\RUN_CLIENT.bat live
```

Chỉ Client được thay; nếu Backend chưa hỗ trợ đúng bốn endpoint trên, Client sẽ báo chưa kết nối thay vì hiển thị số liệu ảo.

## Kịch bản nghiệm thu 4 thiết bị

1. Bốn điện thoại/máy tính mở website cùng IP của laptop, số **Đang truy cập = 4** (nếu các phiên đều gửi heartbeat thành công, chưa hết hạn; tài khoản admin quản lý ở một tab riêng).
2. Hai máy phát audio thật: **Đang nghe = 2**, **Đang truy cập = 4**.
3. Một máy tạm dừng: **Đang nghe = 1**, **Đang truy cập = 4**.
4. Một máy đóng tab: số truy cập giảm sau tín hiệu `/end` hoặc sau **tối đa ~30 giây + tối đa 5 giây** làm mới Dashboard.

## CSP, xác thực, quyền riêng tư

- HTML được Java tạo; JS nằm trong `assets/visitor-activity.js` và `assets/admin-visitors.js`, CSP cho phép `script-src 'self'`, không dùng script nội bộ.
- Trang tổng hợp quản trị yêu cầu phiên admin Client hợp lệ; Java kiểm tra với `auth-service` và gửi tiếp **cookie admin phía server** cho visitor-service.
- Khách không cần tài khoản. Không thu thập danh tính, GPS hoặc thông tin cá nhân để đếm số phiên.
- DEMO chỉ là mô phỏng API bằng Java trong RAM. Dữ liệu LIVE do Backend quản lý; Client không tạo thêm database/Backend.
