# Saigon Audio Guide — Luồng đường đi tân sinh viên

## Phạm vi
- Client (Java): `/event-routes` danh sách, `/event-route?id=...` chi tiết dạng luồng bước.
- Admin (đăng nhập): `/admin/event-routes` và `/admin/event-route?id=...` để tạo/sửa/xóa, công bố/ẩn, đánh dấu nội dung mẫu, thay thứ tự bước và chọn POI thật.
- POI service (`8082`): API GET `/api/event-routes`, GET `/api/event-routes/{id}`; GET/POST `/api/admin/event-routes`, GET/PUT/DELETE `/api/admin/event-routes/{id}`.
- CSDL MySQL: hai bảng `campus_event_routes`, `campus_event_stops`; không sửa bảng POI hoặc tuyến du lịch cũ.

## Dữ liệu mẫu
1. Thủ tục nhập học trực tiếp (5 bước).
2. Khám sức khỏe nhập học (5 bước).
3. Sinh hoạt và làm quen trường (4 bước).

**Quan trọng:** Đây là luồng minh họa, KHÔNG phải thông báo chính thức của SGU. Phòng, số bàn, giấy tờ và thời gian thực tế phụ thuộc từng đợt. Những bước chưa được gắn với POI chỉ hiện mô tả, không giả lập đường đi hoặc tọa độ.

## Cài đặt (Windows PowerShell / VS Code)

1. Sao lưu project (đặc biệt `app.css`, `Layout.java`, `VisitorView.java`, `ApiDAO.java`, `ClientController.java`). Bản vá ZIP ghi đè các file tương ứng nên chỉ áp dụng lên đúng phiên bản project được đính kèm. Có thể dùng file `.patch` thay cho ZIP nếu đã có chỉnh sửa cục bộ; chạy `git apply --check` trước.
2. Giải nén `SGU_Freshman_Event_Routes_Patch.zip` vào thư mục gốc `Project_CNPM`, giữ nguyên các đường dẫn.
3. Trong **MySQL Workbench** đang kết nối database đã import trước đó, mở `Database/2026_freshman_event_routes.sql`, kiểm tra lệnh `USE sgu_audio_guide;` trỏ đúng schema, chạy toàn bộ **một lần**. Script chỉ tạo bảng mới, không DROP bảng cũ. Lặp lại script không nhân đôi 3 tuyến mẫu.
4. Dừng riêng `poi-service` trên cổng **8082** (đừng dừng toàn bộ Java). Nếu service cũ còn chạy, bản build mới sẽ không có API này.
5. Build các module từ thư mục gốc:

```powershell
.\mvnw.cmd -f srcackend\poi-service\pom.xml -DskipTests package
.\mvnw.cmd -f src\client\pom.xml -DskipTests package
```

6. Khởi động lại `poi-service` với cấu hình MySQL/auth đang dùng (hoặc dùng `START_BACKEND.ps1` sau khi dừng service cũ). Dừng riêng Client cũ trên cổng **8080**, rồi chạy Client mới. Nếu build báo file JAR bị khóa, kiểm tra tiến trình Java đang dùng đúng cổng trước khi dừng.

```powershell
cd src\client
$env:MAPTILER_API_KEY = Read-Host "MapTiler key (de trong neu khong dung ban do nen)"
java -jar target\Client.jar
```

7. Mở `http://localhost:8080/event-routes`. Đăng nhập quản trị rồi mở `http://localhost:8080/admin/event-routes`.

## Kiểm tra

```powershell
curl.exe -i http://127.0.0.1:8082/api/event-routes
curl.exe -i http://127.0.0.1:8080/api/event-routes
```

API mong đợi `200` và 3 lộ trình mẫu. Nếu `500`, xác nhận đã chạy SQL trên đúng database của poi-service. Nếu `404`, build và khởi động lại poi-service/gateway.

Trong MySQL Workbench:

```sql
USE sgu_audio_guide;
SELECT id,route_code,title,is_published,is_template FROM campus_event_routes;
SELECT route_id,sort_order,title,location_label,poi_id FROM campus_event_stops ORDER BY route_id,sort_order;
```

## Cách xác nhận vị trí các bước

Vào Admin -> Quản lý địa điểm, tạo POI thật (tọa độ từ nguồn đáng tin cậy) cho quầy nhập học/điểm khám nếu được SGU xác nhận. Vào Admin -> Đường đi sự kiện -> Chỉnh sửa, tại mỗi bước chọn POI vừa tạo, sửa địa điểm/lịch theo đợt; chỉ sau khi xác minh mới bỏ dấu **lộ trình mẫu**. Trên Client, nút **Xem POI trên bản đồ** chỉ có ở bước đã gắn POI còn hiển thị công khai.

## Giới hạn
- Đây là hướng dẫn tuần tự theo bước, chưa phải thuật toán chỉ đường đi bộ giữa các phòng/tòa nhà hay GPS turn-by-turn.
- Chưa kiểm thử tích hợp MySQL/Backend trên Windows; đã biên dịch toàn bộ Client bằng JDK 21, kiểm tra cú pháp Java mới của Backend và render HTML mẫu.
- Không thay đổi chức năng bản đồ/GPS hiện hữu; chỉ thêm liên kết điều hướng tới mục Đường đi.
