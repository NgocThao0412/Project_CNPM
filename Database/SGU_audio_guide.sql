CREATE DATABASE IF NOT EXISTS SGU_audio_guide
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE SGU_audio_guide;
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

CREATE TABLE admins (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  auth_version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Backend tang khi dang xuat/reset; token cu bi vo hieu',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_admin_username (username),
  UNIQUE KEY uq_admin_email (email),
  CONSTRAINT ck_admin_username CHECK (CHAR_LENGTH(TRIM(username)) > 0),
  CONSTRAINT ck_admin_email CHECK (CHAR_LENGTH(TRIM(email)) > 0),
  CONSTRAINT ck_admin_password CHECK (CHAR_LENGTH(TRIM(password_hash)) > 0),
  CONSTRAINT ck_admin_name CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
  CONSTRAINT ck_admin_active CHECK (is_active IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE password_reset_tokens (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  admin_id BIGINT UNSIGNED NOT NULL,
  token_hash BINARY(32) NOT NULL COMMENT 'SHA-256 cua reset request token ngau nhien',
  otp_hash BINARY(32) NOT NULL COMMENT 'HMAC-SHA256(request token + OTP), khoa bi mat chi o backend',
  failed_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
  verified_at DATETIME(6) NULL COMMENT 'Thoi diem OTP duoc xac thuc; chua phai da doi mat khau',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  UNIQUE KEY uq_reset_token (token_hash),
  KEY ix_reset_admin (admin_id, expires_at),
  CONSTRAINT fk_reset_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE,
  CONSTRAINT ck_reset_expiry CHECK (expires_at > created_at),
  CONSTRAINT ck_reset_attempts CHECK (failed_attempts BETWEEN 0 AND 5),
  CONSTRAINT ck_reset_verified CHECK (verified_at IS NULL OR (verified_at >= created_at AND verified_at <= expires_at)),
  CONSTRAINT ck_reset_consumed CHECK (used_at IS NULL OR (verified_at IS NOT NULL AND used_at >= verified_at)),
  CONSTRAINT ck_reset_used CHECK (used_at IS NULL OR (used_at >= created_at AND used_at <= expires_at))
) ENGINE=InnoDB;

CREATE TABLE languages (
  id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  language_code VARCHAR(35) NOT NULL COMMENT 'Vi du vi, en; ma ngon ngu thay cho quoc ky',
  language_name VARCHAR(100) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_language_code (language_code),
  CONSTRAINT ck_language_code CHECK (CHAR_LENGTH(TRIM(language_code)) > 0),
  CONSTRAINT ck_language_name CHECK (CHAR_LENGTH(TRIM(language_name)) > 0),
  CONSTRAINT ck_language_active CHECK (is_active IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE campus_areas (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  area_code VARCHAR(50) NOT NULL,
  area_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_area_code (area_code),
  CONSTRAINT ck_area_code CHECK (CHAR_LENGTH(TRIM(area_code)) > 0),
  CONSTRAINT ck_area_name CHECK (CHAR_LENGTH(TRIM(area_name)) > 0),
  CONSTRAINT ck_area_visible CHECK (is_visible IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE points_of_interest (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  area_id BIGINT UNSIGNED NOT NULL,
  poi_code VARCHAR(50) NOT NULL,
  poi_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  trigger_radius_m DECIMAL(8,2) NOT NULL COMMENT 'Ban kinh theo met; cau hinh rieng, khong mac dinh 50m',
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_poi_code (poi_code),
  KEY ix_poi_name (poi_name),
  KEY ix_poi_area_visible (area_id, is_visible),
  CONSTRAINT fk_poi_area FOREIGN KEY (area_id) REFERENCES campus_areas(id) ON DELETE RESTRICT,
  CONSTRAINT ck_poi_code CHECK (CHAR_LENGTH(TRIM(poi_code)) > 0),
  CONSTRAINT ck_poi_name CHECK (CHAR_LENGTH(TRIM(poi_name)) > 0),
  CONSTRAINT ck_poi_lat CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT ck_poi_lon CHECK (longitude BETWEEN -180 AND 180),
  CONSTRAINT ck_poi_radius CHECK (trigger_radius_m > 0),
  CONSTRAINT ck_poi_visible CHECK (is_visible IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE poi_images (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  poi_id BIGINT UNSIGNED NOT NULL,
  file_url VARCHAR(2048) NOT NULL,
  caption VARCHAR(255) NULL,
  sort_order SMALLINT UNSIGNED NOT NULL COMMENT 'Anh dau tien la anh dai dien',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_image_position (poi_id, sort_order),
  CONSTRAINT fk_image_poi FOREIGN KEY (poi_id) REFERENCES points_of_interest(id) ON DELETE CASCADE,
  CONSTRAINT ck_image_path CHECK (CHAR_LENGTH(TRIM(file_url)) > 0),
  CONSTRAINT ck_image_position CHECK (sort_order > 0)
) ENGINE=InnoDB;

CREATE TABLE narration_contents (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  poi_id BIGINT UNSIGNED NOT NULL,
  language_id SMALLINT UNSIGNED NOT NULL,
  title VARCHAR(200) NOT NULL,
  content LONGTEXT NOT NULL,
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_narration_poi_language (poi_id, language_id),
  KEY ix_narration_language_visible (language_id, is_visible),
  KEY ix_narration_title (title),
  CONSTRAINT fk_narration_poi FOREIGN KEY (poi_id) REFERENCES points_of_interest(id) ON DELETE RESTRICT,
  CONSTRAINT fk_narration_language FOREIGN KEY (language_id) REFERENCES languages(id) ON DELETE RESTRICT,
  CONSTRAINT ck_narration_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
  CONSTRAINT ck_narration_content CHECK (CHAR_LENGTH(TRIM(content)) > 0),
  CONSTRAINT ck_narration_visible CHECK (is_visible IN (0,1))
) ENGINE=InnoDB;

-- Mot noi dung co the giu nhieu phien ban audio, toi da mot ban dang hien thi.
-- Khi thay audio: an ban cu va tao ban moi trong cung transaction.
CREATE TABLE audio_files (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  content_id BIGINT UNSIGNED NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(2048) NOT NULL,
  mime_type VARCHAR(100) NOT NULL DEFAULT 'audio/mpeg',
  file_size_bytes BIGINT UNSIGNED NOT NULL,
  duration_seconds DECIMAL(10,2) NOT NULL,
  version_number SMALLINT UNSIGNED NOT NULL,
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  active_content_id BIGINT UNSIGNED
    GENERATED ALWAYS AS (CASE WHEN is_visible = 1 THEN content_id ELSE NULL END) STORED,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_audio_version (content_id, version_number),
  UNIQUE KEY uq_audio_active_content (active_content_id),
  CONSTRAINT fk_audio_content FOREIGN KEY (content_id) REFERENCES narration_contents(id) ON DELETE RESTRICT,
  CONSTRAINT ck_audio_filename CHECK (CHAR_LENGTH(TRIM(file_name)) > 0),
  CONSTRAINT ck_audio_path CHECK (CHAR_LENGTH(TRIM(file_url)) > 0),
  CONSTRAINT ck_audio_mime CHECK (mime_type = 'audio/mpeg'),
  CONSTRAINT ck_audio_size CHECK (file_size_bytes > 0),
  CONSTRAINT ck_audio_duration CHECK (duration_seconds > 0),
  CONSTRAINT ck_audio_version CHECK (version_number > 0),
  CONSTRAINT ck_audio_visible CHECK (is_visible IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE tour_routes (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  route_code VARCHAR(50) NOT NULL,
  route_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  estimated_duration_minutes SMALLINT UNSIGNED NOT NULL,
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_route_code (route_code),
  CONSTRAINT ck_route_code CHECK (CHAR_LENGTH(TRIM(route_code)) > 0),
  CONSTRAINT ck_route_name CHECK (CHAR_LENGTH(TRIM(route_name)) > 0),
  CONSTRAINT ck_route_duration CHECK (estimated_duration_minutes > 0),
  CONSTRAINT ck_route_visible CHECK (is_visible IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE tour_route_pois (
  route_id BIGINT UNSIGNED NOT NULL,
  poi_id BIGINT UNSIGNED NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL,
  PRIMARY KEY (route_id, poi_id),
  UNIQUE KEY uq_route_position (route_id, sort_order),
  CONSTRAINT fk_routepoi_route FOREIGN KEY (route_id) REFERENCES tour_routes(id) ON DELETE CASCADE,
  CONSTRAINT fk_routepoi_poi FOREIGN KEY (poi_id) REFERENCES points_of_interest(id) ON DELETE RESTRICT,
  CONSTRAINT ck_routepoi_order CHECK (sort_order > 0)
) ENGINE=InnoDB;

-- Phien an danh, khong phai tai khoan khach hay ma mua ve.
-- Backend giu token goc trong cookie an toan; DB chi giu hash.
CREATE TABLE visitor_sessions (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  token_hash BINARY(32) NOT NULL,
  started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at DATETIME(6) NOT NULL,
  ended_at DATETIME(6) NULL,
  UNIQUE KEY uq_visit_token (token_hash),
  KEY ix_visit_expiry (expires_at),
  CONSTRAINT ck_visit_expiry CHECK (expires_at > started_at),
  CONSTRAINT ck_visit_end CHECK (ended_at IS NULL OR ended_at >= started_at)
) ENGINE=InnoDB;

CREATE TABLE listening_logs (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'UUID do backend tao, dung lai khi retry',
  session_id BIGINT UNSIGNED NOT NULL,
  audio_file_id BIGINT UNSIGNED NOT NULL,
  trigger_source ENUM('GPS', 'MANUAL', 'SIMULATION') NOT NULL,
  started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thoi gian UTC do server ghi',
  UNIQUE KEY uq_listen_event (event_id),
  KEY ix_listen_time (started_at),
  KEY ix_listen_audio_time (audio_file_id, started_at),
  KEY ix_listen_visit_time (session_id, started_at),
  KEY ix_listen_source_time (trigger_source, started_at),
  CONSTRAINT fk_listen_audio FOREIGN KEY (audio_file_id) REFERENCES audio_files(id) ON DELETE RESTRICT,
  CONSTRAINT fk_listen_visit FOREIGN KEY (session_id) REFERENCES visitor_sessions(id) ON DELETE RESTRICT,
  CONSTRAINT ck_listen_event CHECK (CHAR_LENGTH(TRIM(event_id)) = 36)
) ENGINE=InnoDB;

DELIMITER $$

-- Khong doi POI/ngon ngu cua mot bai cu de lich su thong ke khong bi gan sai.
-- Muon chuyen sang POI/ngon ngu khac: tao bai moi, an bai cu.
CREATE TRIGGER trg_narration_before_update_identity
BEFORE UPDATE ON narration_contents FOR EACH ROW
BEGIN
  IF NEW.poi_id <> OLD.poi_id OR NEW.language_id <> OLD.language_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc doi POI/ngon ngu cua bai; hay tao bai moi.';
  END IF;
END$$

-- Khong gan lai mot file audio sang noi dung khac vi se lam sai lich su.
CREATE TRIGGER trg_audio_before_update_identity
BEFORE UPDATE ON audio_files FOR EACH ROW
BEGIN
  IF NEW.content_id <> OLD.content_id OR NEW.version_number <> OLD.version_number THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc doi noi dung/phien ban audio; hay tao ban moi.';
  END IF;
  IF (BINARY NEW.file_url <> BINARY OLD.file_url
      OR BINARY NEW.file_name <> BINARY OLD.file_name
      OR NEW.file_size_bytes <> OLD.file_size_bytes
      OR NEW.duration_seconds <> OLD.duration_seconds) AND EXISTS (
    SELECT 1 FROM listening_logs WHERE audio_file_id = OLD.id
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audio da co luot nghe: hay tao phien ban moi khi thay file.';
  END IF;
END$$

-- Chi ghi luot nghe moi cho audio/noi dung/POI/khu vuc/ngon ngu kha dung.
-- Phien phai con han va chua ket thuc.
CREATE TRIGGER trg_listening_before_insert_validate
BEFORE INSERT ON listening_logs FOR EACH ROW
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM audio_files a
    JOIN narration_contents t ON t.id = a.content_id
    JOIN points_of_interest p ON p.id = t.poi_id
    JOIN campus_areas k ON k.id = p.area_id
    JOIN languages n ON n.id = t.language_id
    WHERE a.id = NEW.audio_file_id
      AND a.is_visible = 1 AND t.is_visible = 1
      AND p.is_visible = 1 AND k.is_visible = 1 AND n.is_active = 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bai thuyet minh, POI hoac ngon ngu khong kha dung.';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM visitor_sessions v
    WHERE v.id = NEW.session_id AND v.ended_at IS NULL
      AND v.started_at <= UTC_TIMESTAMP(6) AND v.expires_at > UTC_TIMESTAMP(6)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Phien tham quan khong ton tai, da ket thuc hoac het han.';
  END IF;
  SET NEW.started_at = UTC_TIMESTAMP(6);
END$$

-- Nhat ky la su kien bat dau phat, khong sua su kien cu.
CREATE TRIGGER trg_listening_before_update_immutable
BEFORE UPDATE ON listening_logs FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc sua nhat ky luot nghe.';
END$$

DELIMITER ;

