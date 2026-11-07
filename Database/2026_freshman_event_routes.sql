-- Saigon Audio Guide: event routes for freshmen. Run once on the existing SGU_audio_guide schema.
-- SAFE MIGRATION: creates only new tables; does not DROP existing tables or erase POIs.
-- MySQL 8.4 / utf8mb4. Verify the current schema in Workbench before running.
USE sgu_audio_guide;
CREATE TABLE IF NOT EXISTS campus_event_routes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  route_code VARCHAR(60) NOT NULL UNIQUE,
  title VARCHAR(180) NOT NULL,
  summary TEXT NOT NULL,
  audience VARCHAR(100) NOT NULL DEFAULT 'Tân sinh viên',
  is_published TINYINT(1) NOT NULL DEFAULT 0,
  is_template TINYINT(1) NOT NULL DEFAULT 1,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_campus_route_published CHECK (is_published IN (0,1)),
  CONSTRAINT ck_campus_route_template CHECK (is_template IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS campus_event_stops (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  route_id BIGINT UNSIGNED NOT NULL,
  sort_order INT NOT NULL,
  title VARCHAR(180) NOT NULL,
  instructions TEXT NOT NULL,
  location_label VARCHAR(200) NOT NULL DEFAULT '',
  poi_id BIGINT UNSIGNED NULL,
  duration_minutes INT NOT NULL DEFAULT 0,
  UNIQUE KEY uq_event_route_order (route_id,sort_order),
  KEY idx_event_stop_poi (poi_id),
  CONSTRAINT fk_event_stop_route FOREIGN KEY (route_id) REFERENCES campus_event_routes(id) ON DELETE CASCADE,
  CONSTRAINT fk_event_stop_poi FOREIGN KEY (poi_id) REFERENCES points_of_interest(id) ON DELETE SET NULL,
  CONSTRAINT ck_event_stop_order CHECK (sort_order BETWEEN 1 AND 12),
  CONSTRAINT ck_event_stop_duration CHECK (duration_minutes BETWEEN 0 AND 240)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sample workflows only: school-specific room, timing, requirements and location remain unverified.
INSERT INTO campus_event_routes (route_code,title,summary,audience,is_published,is_template)
VALUES
('NHAP_HOC_TRUC_TIEP','Thủ tục nhập học trực tiếp','Thứ tự tham khảo khi đến trường nộp hồ sơ. Hãy kiểm tra giấy báo và thông báo chính thức của SGU trước khi đến.','Tân sinh viên',1,1),
('KHAM_SUC_KHOE_DAU_KHOA','Khám sức khỏe nhập học','Luồng tham khảo cho đợt khám sức khỏe đầu khóa. Địa điểm và thứ tự bàn khám do nhà trường công bố.','Tân sinh viên',1,1),
('SINH_HOAT_DAU_KHOA','Sinh hoạt và làm quen trường','Các bước tham khảo để tham gia hướng dẫn đầu khóa, nhận thông tin lớp và tìm hiểu dịch vụ sinh viên.','Tân sinh viên',1,1)
ON DUPLICATE KEY UPDATE route_code=route_code;

-- Do not duplicate sample stops when the migration is run again.
INSERT INTO campus_event_stops (route_id,sort_order,title,instructions,location_label,duration_minutes)
SELECT r.id, s.n, s.title, s.instructions, s.location_label, s.duration_minutes
FROM campus_event_routes r
JOIN (
 SELECT 'NHAP_HOC_TRUC_TIEP' code,1 n,'Chuẩn bị và kiểm tra hồ sơ' title,'Đối chiếu giấy báo nhập học và danh mục giấy tờ trong thông báo tuyển sinh chính thức. Mang bản gốc/bản sao theo hướng dẫn.' instructions,'Trước khi đến SGU' location_label,0 duration_minutes
 UNION ALL SELECT 'NHAP_HOC_TRUC_TIEP',2,'Đến điểm hướng dẫn nhập học','Tìm bảng chỉ dẫn hoặc nhân viên hỗ trợ. Xác nhận khu vực tiếp nhận đúng theo ngành và đợt nhập học.','Điểm hướng dẫn (chờ SGU cập nhật)',5
 UNION ALL SELECT 'NHAP_HOC_TRUC_TIEP',3,'Nộp hồ sơ trực tiếp','Lấy số thứ tự nếu có; nộp các giấy tờ được yêu cầu cho bộ phận tiếp nhận.','Quầy tiếp nhận (chờ SGU cập nhật)',15
 UNION ALL SELECT 'NHAP_HOC_TRUC_TIEP',4,'Đối chiếu và bổ sung thông tin','Kiểm tra dữ liệu cá nhân, ghi nhận giấy tờ còn thiếu và thực hiện các yêu cầu bổ sung nếu có.','Khu vực kiểm tra hồ sơ (chờ SGU cập nhật)',10
 UNION ALL SELECT 'NHAP_HOC_TRUC_TIEP',5,'Xác nhận hoàn tất nhập học','Nhận hướng dẫn tiếp theo về lớp học, lịch sinh hoạt và khám sức khỏe; giữ lại giấy xác nhận nếu được cấp.','Điểm hoàn tất thủ tục (chờ SGU cập nhật)',5
 UNION ALL SELECT 'KHAM_SUC_KHOE_DAU_KHOA',1,'Tra cứu lịch khám sức khỏe','Xem ngày, giờ, giấy tờ và phiếu khám trong thông báo chính thức của trường.','Trước ngày khám',0
 UNION ALL SELECT 'KHAM_SUC_KHOE_DAU_KHOA',2,'Đến điểm tiếp nhận','Đến đúng khu vực và giờ hẹn; xuất trình giấy tờ theo yêu cầu.','Điểm tiếp nhận khám (chờ SGU cập nhật)',5
 UNION ALL SELECT 'KHAM_SUC_KHOE_DAU_KHOA',3,'Đăng ký và nhận hướng dẫn','Xác nhận thông tin, nhận phiếu khám hoặc số thứ tự theo quy trình do đơn vị tổ chức hướng dẫn.','Bàn đăng ký (chờ SGU cập nhật)',10
 UNION ALL SELECT 'KHAM_SUC_KHOE_DAU_KHOA',4,'Khám theo chỉ dẫn','Thực hiện từng hạng mục theo thứ tự thực tế được nhân viên y tế hướng dẫn; không tự ý bỏ qua bước.','Các bàn khám (chờ SGU cập nhật)',40
 UNION ALL SELECT 'KHAM_SUC_KHOE_DAU_KHOA',5,'Nộp phiếu và hoàn tất','Kiểm tra đã hoàn thành các hạng mục và nộp lại phiếu khám tại nơi được hướng dẫn.','Điểm hoàn tất khám (chờ SGU cập nhật)',10
 UNION ALL SELECT 'SINH_HOAT_DAU_KHOA',1,'Xem thông báo sinh hoạt đầu khóa','Đọc lịch, danh sách và địa điểm sinh hoạt trong kênh thông tin chính thức của SGU.','Tra cứu online',0
 UNION ALL SELECT 'SINH_HOAT_DAU_KHOA',2,'Đến điểm tập trung','Đến địa điểm theo lớp/ngành, điểm danh hoặc check-in theo hướng dẫn.','Điểm tập trung (chờ SGU cập nhật)',10
 UNION ALL SELECT 'SINH_HOAT_DAU_KHOA',3,'Tham gia sinh hoạt','Nghe phổ biến quy chế, các kênh liên hệ và lưu ý dành cho tân sinh viên.','Phòng/hội trường (chờ SGU cập nhật)',45
 UNION ALL SELECT 'SINH_HOAT_DAU_KHOA',4,'Nhận thông tin tiếp theo','Ghi lại lịch học, hoạt động và các thủ tục còn phải thực hiện.','Theo hướng dẫn từ nhà trường',5
) s ON s.code=r.route_code
WHERE NOT EXISTS (SELECT 1 FROM campus_event_stops st WHERE st.route_id=r.id);
