# NY — CLIENT | SGU Audio Guide (Nhóm 12)

Frontend độc lập thực hiện **4 đầu việc của Phạm Thị Bảo Ny** theo bảng phân công:

1. Form đăng nhập, thông báo lỗi, đăng xuất và khung quản trị.
2. Trang quản lý POI: danh sách/tìm kiếm, thêm/sửa, ẩn/hiện, cấu hình GPS, URL ảnh.
3. Trang quản lý nội dung thuyết minh: theo POI + ngôn ngữ, thêm/sửa, ẩn/hiện, tải/thay thế audio, nghe thử.
4. Trang khách không cần đăng nhập: bản đồ, chọn POI mô phỏng, định vị GPS, chọn ngôn ngữ, xem nội dung, phát audio, đọc thử TTS, lộ trình gợi ý.

## 1. Chạy thử (không cần Node.js hay database)

Mở Terminal trong thư mục `Ny_Client`, chạy:

```bash
python -m http.server 5500
```

Nếu đã cài Node.js, có thể chạy `npm run serve` (vẫn dùng Python để phục vụ trang) và `npm test` để chạy bộ kiểm thử demo.

Truy cập **http://localhost:5500**. Không nên mở trực tiếp bằng `file://` vì JavaScript ES Modules cần HTTP.

- Trang khách: `http://localhost:5500/#/visitor`
- Trang admin: `http://localhost:5500/#/admin/login`
- **Demo:** `admin@sgu.edu.vn` / `123456` — chỉ là tài khoản giả lập chạy trên trình duyệt.
- Dữ liệu demo tự lưu ở `localStorage`. Muốn xóa dữ liệu: DevTools → Application → Local Storage → xóa khóa `ny-client-demo-v1` và tải lại trang.
- Dữ liệu tên/địa điểm/toạ độ **mang tính minh họa**, không được coi là kết quả khảo sát GPS của trường.
- Để hiện nền bản đồ thật cần Internet (Leaflet CDN và OpenStreetMap). Khi không tải được, ứng dụng hiển thị **sơ đồ vị trí tương đối** và vẫn cho phép chọn POI.
- File audio đã tải lên khi chạy demo chỉ lưu tạm trong **phiên trang hiện tại**; sau khi tải lại cần tải file lại. Nếu chưa có file audio, nút TTS sử dụng giọng đọc trình duyệt, không phải MP3 đã lưu trong CSDL.
- Định vị GPS cần được cấp quyền, nên chạy trên `localhost` hoặc HTTPS.

## 2. Kết nối Backend của nhóm

Trong `index.html`, thay:

```js
window.NY_CONFIG = { mode: 'demo', apiBase: 'http://localhost:8000/api' };
```

bằng:

```js
window.NY_CONFIG = { mode: 'api', apiBase: 'http://localhost:8000/api' };
```

Thay URL theo cổng backend của nhóm. Các đường dẫn dưới đây là **API contract đề xuất**, KHÔNG phải các endpoint đã kiểm chứng trong GitHub. Cần đồng bộ với người làm backend rồi sửa **duy nhất `src/api.js`** nếu đường dẫn/field khác.

| Method | Endpoint | Chức năng |
|---|---|---|
| POST | `/auth/login` | `{email,password}` → `{token,user:{name,email}}` |
| POST | `/auth/logout` | Hủy phiên/tokens phía server |
| GET | `/public/pois` | Danh sách POI hiển thị (không cần auth) |
| GET | `/pois` | Danh sách POI dành cho admin (Bearer token) |
| POST | `/pois` | Thêm POI |
| PUT | `/pois/:id` | Sửa POI |
| PATCH | `/pois/:id/visibility` | `{is_visible:boolean}` |
| GET | `/languages` | Danh sách ngôn ngữ |
| GET | `/public/narrations` | Danh sách nội dung đang hiển thị |
| GET | `/narrations` | Danh sách nội dung dành cho admin |
| POST | `/narrations` | Thêm nội dung |
| PUT | `/narrations/:id` | Sửa nội dung |
| PATCH | `/narrations/:id/visibility` | `{is_visible:boolean}` |
| POST | `/narrations/:id/audio` | `multipart/form-data`, trường `audio` |
| GET | `/public/routes` | Danh sách lộ trình gợi ý (tùy chọn) |
| POST | `/visitor-sessions` | Khởi tạo phiên tham quan ẩn danh, trả `{id}` |
| POST | `/listening-logs` | Ghi nhận lượt phát audio |

API có thể trả `[...]`, `{data:[...]}` hoặc `{data:{items:[...]}}`. Mặc định auth qua `Authorization: Bearer <token>`.

### Cấu trúc dữ liệu mong đợi

```json
{
  "poi": {"id": 1, "code": "SGU-001", "name": "Cổng chính", "area": "Khu trung tâm", "description": "...", "latitude": 10.76012, "longitude": 106.68162, "radius_m": 45, "image_url": "", "is_visible": true},
  "narration": {"id": 1, "poi_id": 1, "language_code": "vi", "title": "Chào mừng", "body": "...", "audio_url": "https://.../file.mp3", "is_visible": true},
  "language": {"code": "vi", "name": "Tiếng Việt"},
  "route": {"id": 1, "name": "Khám phá SGU", "description": "...", "poi_ids": [1,2,3]}
}
```

Nếu backend dùng `area_id`, `latitude/longitude` kiểu khác hoặc dữ liệu audio nằm trong bảng `audio_files`, cần thêm bước ánh xạ trong `src/api.js`. File URL của audio phải được backend cấu hình truy cập phù hợp; không được gắn secret API key vào frontend.

## 3. Cấu trúc project

```
Ny_Client/
├── index.html           # Điểm vào + cấu hình demo/API
├── styles.css           # Giao diện responsive
├── src/
│   ├── app.js           # Trang khách, quản trị, sự kiện & form
│   ├── api.js           # Giao tiếp API; bearer token trong sessionStorage
│   ├── config.js        # Tiện ích & kiểm tra dữ liệu
│   ├── map.js           # Leaflet + bản đồ dự phòng không cần CDN
│   └── mock.js          # Dữ liệu và tác vụ demo (localStorage)
├── tests/mock.test.mjs  # Kiểm thử thao tác và ràng buộc dữ liệu demo
├── package.json         # Lệnh npm test / npm run serve
└── README.md
```

## 4. Chức năng demo cần kiểm thử

- Nhập sai mật khẩu, kiểm tra lỗi; đăng nhập đúng, mở POI; đăng xuất, truy cập admin phải về form đăng nhập.
- Tìm POI; thêm POI, lưu GPS; sửa tên, ẩn POI và xác nhận POI bị loại khỏi trang khách.
- Thêm nội dung theo POI và ngôn ngữ; kiểm tra chặn POI–ngôn ngữ trùng; sửa, ẩn nội dung; tải MP3 và nghe thử.
- Chọn một POI trên bản đồ/danh sách, chuyển vi ↔ en và kiểm tra nội dung; phát audio nếu đã tải; yêu cầu GPS (nếu không cấp quyền phải báo lỗi); test màn hình mobile.
- Kiểm tra khi backend chậm/hỏng; thông báo lỗi mạng và không báo thành công giả.

## 5. Giới hạn / bàn giao

- Đây là **frontend tạo độc lập**, chưa có quyền đọc repo GitHub hoặc Google Drive người dùng gửi nên **chưa được ghép hay push vào repo nhóm**, cũng không thể khẳng định tương thích API thật.
- **Không có chức năng OTP quên mật khẩu** (không nằm trong 4 mục Ny được giao ở ảnh). Nếu thầy yêu cầu giao diện theo UC-3, phải bổ sung sau khi xác nhận API email/OTP.
- Xác thực demo không an toàn và tuyệt đối không dùng trong môi trường production. Backend phải xác thực thật, băm mật khẩu, phân quyền, kiểm tra quyền tại API và thực hiện ẩn/hiện dữ liệu phía server.
- Bộ phát audio chưa thực hiện versioning/báo cáo đầy đủ; backend của nhóm phải xử lý lưu file, phiên bản audio, log nghe, số liệu thống kê.
- Có thể triển khai frontend tĩnh lên GitHub Pages, Nginx hoặc đưa thư mục vào frontend sẵn có sau khi đồng nhất framework và API.
