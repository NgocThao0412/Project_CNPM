-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: sgu_audio_guide
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admins`
--

DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(254) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `full_name` varchar(150) NOT NULL,
  `auth_version` int unsigned NOT NULL DEFAULT '0' COMMENT 'Backend tang khi dang xuat/reset; token cu bi vo hieu',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_admin_username` (`username`),
  UNIQUE KEY `uq_admin_email` (`email`),
  CONSTRAINT `ck_admin_active` CHECK ((`is_active` in (0,1))),
  CONSTRAINT `ck_admin_email` CHECK ((char_length(trim(`email`)) > 0)),
  CONSTRAINT `ck_admin_name` CHECK ((char_length(trim(`full_name`)) > 0)),
  CONSTRAINT `ck_admin_password` CHECK ((char_length(trim(`password_hash`)) > 0)),
  CONSTRAINT `ck_admin_username` CHECK ((char_length(trim(`username`)) > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admins`
--

LOCK TABLES `admins` WRITE;
/*!40000 ALTER TABLE `admins` DISABLE KEYS */;
INSERT INTO `admins` VALUES (1,'admin','admin@example.com','$2a$12$/NcAbzlr4OFzYgojyN9t6e6sNLwrPRf2mQ.EP3KMq4v.RJ.fpJDkO','Admin Demo',0,1,'2026-10-24 22:11:21.077628','2026-10-24 22:11:21.077628');
/*!40000 ALTER TABLE `admins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audio_files`
--

DROP TABLE IF EXISTS `audio_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audio_files` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `content_id` bigint unsigned NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_url` varchar(2048) NOT NULL,
  `mime_type` varchar(100) NOT NULL DEFAULT 'audio/mpeg',
  `file_size_bytes` bigint unsigned NOT NULL,
  `duration_seconds` decimal(10,2) NOT NULL,
  `version_number` smallint unsigned NOT NULL,
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `active_content_id` bigint unsigned GENERATED ALWAYS AS ((case when (`is_visible` = 1) then `content_id` else NULL end)) STORED,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_audio_version` (`content_id`,`version_number`),
  UNIQUE KEY `uq_audio_active_content` (`active_content_id`),
  CONSTRAINT `fk_audio_content` FOREIGN KEY (`content_id`) REFERENCES `narration_contents` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_audio_duration` CHECK ((`duration_seconds` > 0)),
  CONSTRAINT `ck_audio_filename` CHECK ((char_length(trim(`file_name`)) > 0)),
  CONSTRAINT `ck_audio_mime` CHECK ((`mime_type` = _utf8mb4'audio/mpeg')),
  CONSTRAINT `ck_audio_path` CHECK ((char_length(trim(`file_url`)) > 0)),
  CONSTRAINT `ck_audio_size` CHECK ((`file_size_bytes` > 0)),
  CONSTRAINT `ck_audio_version` CHECK ((`version_number` > 0)),
  CONSTRAINT `ck_audio_visible` CHECK ((`is_visible` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audio_files`
--

LOCK TABLES `audio_files` WRITE;
/*!40000 ALTER TABLE `audio_files` DISABLE KEYS */;
INSERT INTO `audio_files` (`id`, `content_id`, `file_name`, `file_url`, `mime_type`, `file_size_bytes`, `duration_seconds`, `version_number`, `is_visible`, `created_at`, `updated_at`) VALUES (1,1,'tts-1-vi.mp3','http://localhost:8084/api/audio/files/1.mp3','audio/mpeg',344736,28.73,1,0,'2026-10-25 00:54:01.622531','2026-10-25 01:39:39.000521'),(2,2,'tts-2-en.mp3','http://localhost:8084/api/audio/files/2.mp3','audio/mpeg',353664,29.47,1,0,'2026-10-25 01:13:30.005088','2026-10-25 01:39:57.157342'),(3,3,'tts-3-zh.mp3','http://localhost:8084/api/audio/files/3.mp3','audio/mpeg',360000,30.00,1,0,'2026-10-25 01:13:39.683412','2026-10-25 01:40:12.858444'),(4,4,'tts-4-ko.mp3','http://localhost:8084/api/audio/files/4.mp3','audio/mpeg',363744,30.31,1,0,'2026-10-25 01:13:50.575466','2026-10-25 01:40:27.716581'),(5,1,'tts-1-vi.mp3','http://localhost:8084/api/audio/files/5.mp3','audio/mpeg',310176,25.85,2,1,'2026-10-25 01:39:39.009535','2026-10-25 01:39:39.010670'),(6,2,'tts-2-en.mp3','http://localhost:8084/api/audio/files/6.mp3','audio/mpeg',357696,29.81,2,1,'2026-10-25 01:39:57.158791','2026-10-25 01:39:57.160024'),(7,3,'tts-3-zh.mp3','http://localhost:8084/api/audio/files/7.mp3','audio/mpeg',347616,28.97,2,1,'2026-10-25 01:40:12.862652','2026-10-25 01:40:12.865248'),(8,4,'tts-4-ko.mp3','http://localhost:8084/api/audio/files/8.mp3','audio/mpeg',356256,29.69,2,1,'2026-10-25 01:40:27.718018','2026-10-25 01:40:27.719207'),(9,6,'tts-6-vi.mp3','http://localhost:8084/api/audio/files/9.mp3','audio/mpeg',325440,27.12,1,1,'2026-10-25 01:45:41.187960','2026-10-25 01:45:41.189689'),(10,7,'tts-7-en.mp3','http://localhost:8084/api/audio/files/10.mp3','audio/mpeg',347040,28.92,1,1,'2026-10-25 01:45:53.123226','2026-10-25 01:45:53.125496'),(11,8,'tts-8-zh.mp3','http://localhost:8084/api/audio/files/11.mp3','audio/mpeg',346176,28.85,1,1,'2026-10-25 01:55:15.770029','2026-10-25 01:55:15.771199'),(12,9,'tts-9-ko.mp3','http://localhost:8084/api/audio/files/12.mp3','audio/mpeg',349056,29.09,1,1,'2026-10-25 02:01:25.970080','2026-10-25 02:01:25.973223');
/*!40000 ALTER TABLE `audio_files` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_audio_before_update_identity` BEFORE UPDATE ON `audio_files` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `campus_areas`
--

DROP TABLE IF EXISTS `campus_areas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campus_areas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `area_code` varchar(50) NOT NULL,
  `area_name` varchar(200) NOT NULL,
  `description` text,
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_area_code` (`area_code`),
  CONSTRAINT `ck_area_code` CHECK ((char_length(trim(`area_code`)) > 0)),
  CONSTRAINT `ck_area_name` CHECK ((char_length(trim(`area_name`)) > 0)),
  CONSTRAINT `ck_area_visible` CHECK ((`is_visible` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campus_areas`
--

LOCK TABLES `campus_areas` WRITE;
/*!40000 ALTER TABLE `campus_areas` DISABLE KEYS */;
INSERT INTO `campus_areas` VALUES (1,'SGU_MAIN','Khuôn viên Trường Đại học Sài Gòn','Khu vực quản lý các địa điểm thuyết minh của trường.',1,'2026-10-25 00:09:53.161336','2026-10-25 00:09:53.161336');
/*!40000 ALTER TABLE `campus_areas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `languages`
--

DROP TABLE IF EXISTS `languages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `languages` (
  `id` smallint unsigned NOT NULL AUTO_INCREMENT,
  `language_code` varchar(35) NOT NULL COMMENT 'Vi du vi, en; ma ngon ngu thay cho quoc ky',
  `language_name` varchar(100) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_language_code` (`language_code`),
  CONSTRAINT `ck_language_active` CHECK ((`is_active` in (0,1))),
  CONSTRAINT `ck_language_code` CHECK ((char_length(trim(`language_code`)) > 0)),
  CONSTRAINT `ck_language_name` CHECK ((char_length(trim(`language_name`)) > 0))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `languages`
--

LOCK TABLES `languages` WRITE;
/*!40000 ALTER TABLE `languages` DISABLE KEYS */;
INSERT INTO `languages` VALUES (1,'vi','Tiếng Việt',1),(2,'en','Tiếng Anh',1),(3,'zh','Tiếng Trung',1),(4,'ko','Tiếng Hàn',1);
/*!40000 ALTER TABLE `languages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `listening_logs`
--

DROP TABLE IF EXISTS `listening_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `listening_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `event_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'UUID do backend tao, dung lai khi retry',
  `session_id` bigint unsigned NOT NULL,
  `audio_file_id` bigint unsigned NOT NULL,
  `trigger_source` enum('GPS','SIMULATION') NOT NULL COMMENT 'Nguồn xác định POI: GPS thực tế hoặc vị trí mô phỏng; không phân loại theo nút Play',
  `started_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thoi gian UTC do server ghi',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_listen_event` (`event_id`),
  KEY `ix_listen_time` (`started_at`),
  KEY `ix_listen_audio_time` (`audio_file_id`,`started_at`),
  KEY `ix_listen_visit_time` (`session_id`,`started_at`),
  KEY `ix_listen_source_time` (`trigger_source`,`started_at`),
  CONSTRAINT `fk_listen_audio` FOREIGN KEY (`audio_file_id`) REFERENCES `audio_files` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_listen_visit` FOREIGN KEY (`session_id`) REFERENCES `visitor_sessions` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_listen_event` CHECK ((char_length(trim(`event_id`)) = 36))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `listening_logs`
--

LOCK TABLES `listening_logs` WRITE;
/*!40000 ALTER TABLE `listening_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `listening_logs` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_listening_before_insert_validate` BEFORE INSERT ON `listening_logs` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_listening_before_update_immutable` BEFORE UPDATE ON `listening_logs` FOR EACH ROW BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc sua nhat ky luot nghe.';
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `narration_contents`
--

DROP TABLE IF EXISTS `narration_contents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `narration_contents` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `poi_id` bigint unsigned NOT NULL,
  `language_id` smallint unsigned NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` longtext NOT NULL,
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_narration_poi_language` (`poi_id`,`language_id`),
  KEY `ix_narration_language_visible` (`language_id`,`is_visible`),
  KEY `ix_narration_title` (`title`),
  CONSTRAINT `fk_narration_language` FOREIGN KEY (`language_id`) REFERENCES `languages` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_narration_poi` FOREIGN KEY (`poi_id`) REFERENCES `points_of_interest` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_narration_content` CHECK ((char_length(trim(`content`)) > 0)),
  CONSTRAINT `ck_narration_title` CHECK ((char_length(trim(`title`)) > 0)),
  CONSTRAINT `ck_narration_visible` CHECK ((`is_visible` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `narration_contents`
--

LOCK TABLES `narration_contents` WRITE;
/*!40000 ALTER TABLE `narration_contents` DISABLE KEYS */;
INSERT INTO `narration_contents` VALUES (1,1,1,'Khám phá sân bóng đá SGU','Chào mừng bạn đến với sân bóng đá trong khuôn viên Trường Đại học Sài Gòn. Tại điểm dừng này, hãy dành một chút thời gian quan sát sân và không gian xung quanh. Nếu đang có trận đấu hoặc buổi tập, bạn hãy đứng ngoài đường biên, giữ khoảng cách an toàn và tránh đi ngang qua sân. Khi tiếp tục tham quan, hãy kiểm tra bản đồ để chọn địa điểm tiếp theo. Bạn có thể chuyển ngôn ngữ để đọc và nghe bài giới thiệu theo lựa chọn của mình.',1,'2026-10-25 00:37:36.041959','2026-10-25 01:39:30.527082'),(2,1,2,'Explore SGU Football Field','Welcome to the football field on the campus of Saigon University. At this stop, take a moment to observe the field and the surrounding space. If a match or practice session is taking place, please stand outside the sideline, keep a safe distance, and avoid walking across the field. When you continue your tour, check the map to choose your next location. You can switch languages to read and listen to the introduction according to your preference.',1,'2026-10-25 01:13:13.164874','2026-10-25 01:39:46.835804'),(3,1,3,'探索西贡大学足球场','欢迎来到西贡大学校园内的足球场。在此停留时，请花点时间观察球场及周边的空间。如果正在进行比赛或训练，请站在边线外，保持安全距离，避免横穿球场。继续参观时，请查看地图以选择下一个地点。您可以切换语言，根据自己的选择阅读和收听介绍。',1,'2026-10-25 01:13:31.235611','2026-10-25 01:40:03.823840'),(4,1,4,'SGU 축구장 탐방','사이공 대학교 캠퍼스 내 축구장에 오신 것을 환영합니다. 이 정거장에서는 잠시 시간을 내어 축구장과 주변 공간을 둘러보세요. 경기나 훈련이 진행 중인 경우, 터치라인 밖에서 안전거리를 유지하고 경기장을 가로질러 이동하지 마십시오. 관람을 계속하려면 지도를 확인하여 다음 장소를 선택하십시오. 원하시는 언어로 소개를 읽고 들으려면 언어를 변경하실 수 있습니다.',1,'2026-10-25 01:13:41.379891','2026-10-25 01:40:19.587767'),(6,2,1,'Khám phá sân bóng đá SGU','Chào mừng bạn đến với sân bóng đá trong khuôn viên Trường Đại học Sài Gòn. Tại điểm dừng này, hãy dành một chút thời gian quan sát sân và không gian xung quanh. Nếu đang có trận đấu hoặc buổi tập, bạn hãy đứng ngoài đường biên, giữ khoảng cách an toàn và tránh đi ngang qua sân. Khi tiếp tục tham quan, hãy kiểm tra bản đồ để chọn địa điểm tiếp theo. Bạn có thể chuyển ngôn ngữ để đọc và nghe bài giới thiệu theo lựa chọn của mình.',1,'2026-10-25 01:45:12.679537','2026-10-25 01:45:12.679537'),(7,2,2,'Explore the SGU Football Field','Welcome to the football field on the campus of Saigon University. At this stop, take a moment to observe the field and the surrounding space. If a match or practice session is taking place, please stand outside the sideline, keep a safe distance, and avoid walking across the field. When you continue your tour, check the map to choose your next destination. You can change the language to read and listen to the introduction according to your preference.',1,'2026-10-25 01:45:44.844300','2026-10-25 01:45:44.844300'),(8,2,3,'探索西贡大学足球场','欢迎来到西贡大学校园内的足球场。在此停留点，请花点时间观察球场及周围空间。如果正在进行比赛或训练，请站在边线外，保持安全距离并避免横穿球场。继续参观时，请查看地图以选择下一个地点。您可以切换语言，根据您的喜好阅读和收听介绍。',1,'2026-10-25 01:45:54.388873','2026-10-25 01:45:54.388873'),(9,2,4,'SGU 축구장 탐방','사이공 대학교 캠퍼스 내 축구장에 오신 것을 환영합니다. 이 장소에서 잠시 멈추어 경기장과 주변 공간을 둘러보세요. 경기나 훈련이 진행 중인 경우, 터치라인 밖에 서서 안전거리를 유지하고 경기장을 가로질러 가지 않도록 하세요. 관람을 계속하려면 지도를 확인하여 다음 장소를 선택하세요. 원하는 언어로 소개를 읽고 들으려면 언어를 변경할 수 있습니다.',1,'2026-10-25 01:45:59.323472','2026-10-25 01:45:59.323472');
/*!40000 ALTER TABLE `narration_contents` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_narration_before_update_identity` BEFORE UPDATE ON `narration_contents` FOR EACH ROW BEGIN
  IF NEW.poi_id <> OLD.poi_id OR NEW.language_id <> OLD.language_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc doi POI/ngon ngu cua bai; hay tao bai moi.';
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `admin_id` bigint unsigned NOT NULL,
  `token_hash` binary(32) NOT NULL COMMENT 'SHA-256 cua reset request token ngau nhien',
  `otp_hash` binary(32) NOT NULL COMMENT 'HMAC-SHA256(request token + OTP), khoa bi mat chi o backend',
  `failed_attempts` tinyint unsigned NOT NULL DEFAULT '0',
  `verified_at` datetime(6) DEFAULT NULL COMMENT 'Thoi diem OTP duoc xac thuc; chua phai da doi mat khau',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `expires_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reset_token` (`token_hash`),
  KEY `ix_reset_admin` (`admin_id`,`expires_at`),
  CONSTRAINT `fk_reset_admin` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_reset_attempts` CHECK ((`failed_attempts` between 0 and 5)),
  CONSTRAINT `ck_reset_consumed` CHECK (((`used_at` is null) or ((`verified_at` is not null) and (`used_at` >= `verified_at`)))),
  CONSTRAINT `ck_reset_expiry` CHECK ((`expires_at` > `created_at`)),
  CONSTRAINT `ck_reset_used` CHECK (((`used_at` is null) or ((`used_at` >= `created_at`) and (`used_at` <= `expires_at`)))),
  CONSTRAINT `ck_reset_verified` CHECK (((`verified_at` is null) or ((`verified_at` >= `created_at`) and (`verified_at` <= `expires_at`))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `poi_images`
--

DROP TABLE IF EXISTS `poi_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `poi_images` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `poi_id` bigint unsigned NOT NULL,
  `file_url` varchar(2048) NOT NULL,
  `caption` varchar(255) DEFAULT NULL,
  `sort_order` smallint unsigned NOT NULL COMMENT 'Anh dau tien la anh dai dien',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_image_position` (`poi_id`,`sort_order`),
  CONSTRAINT `fk_image_poi` FOREIGN KEY (`poi_id`) REFERENCES `points_of_interest` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_image_path` CHECK ((char_length(trim(`file_url`)) > 0)),
  CONSTRAINT `ck_image_position` CHECK ((`sort_order` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `poi_images`
--

LOCK TABLES `poi_images` WRITE;
/*!40000 ALTER TABLE `poi_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `poi_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `points_of_interest`
--

DROP TABLE IF EXISTS `points_of_interest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_of_interest` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `area_id` bigint unsigned NOT NULL,
  `poi_code` varchar(50) NOT NULL,
  `poi_name` varchar(200) NOT NULL,
  `description` text,
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `trigger_radius_m` decimal(8,2) NOT NULL COMMENT 'Ban kinh theo met; cau hinh rieng, khong mac dinh 50m',
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_poi_code` (`poi_code`),
  KEY `ix_poi_name` (`poi_name`),
  KEY `ix_poi_area_visible` (`area_id`,`is_visible`),
  CONSTRAINT `fk_poi_area` FOREIGN KEY (`area_id`) REFERENCES `campus_areas` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_poi_code` CHECK ((char_length(trim(`poi_code`)) > 0)),
  CONSTRAINT `ck_poi_lat` CHECK ((`latitude` between -(90) and 90)),
  CONSTRAINT `ck_poi_lon` CHECK ((`longitude` between -(180) and 180)),
  CONSTRAINT `ck_poi_name` CHECK ((char_length(trim(`poi_name`)) > 0)),
  CONSTRAINT `ck_poi_radius` CHECK ((`trigger_radius_m` > 0)),
  CONSTRAINT `ck_poi_visible` CHECK ((`is_visible` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `points_of_interest`
--

LOCK TABLES `points_of_interest` WRITE;
/*!40000 ALTER TABLE `points_of_interest` DISABLE KEYS */;
INSERT INTO `points_of_interest` VALUES (1,1,'SGU_SAN_BONG_CHUYEN','Sân bóng chuyền','Chào mừng bạn đến với sân bóng đá trong khuôn viên Trường Đại học Sài Gòn. Tại điểm dừng này, hãy dành một chút thời gian quan sát sân và không gian xung quanh. Nếu đang có trận đấu hoặc buổi tập, bạn hãy đứng ngoài đường biên, giữ khoảng cách an toàn và tránh đi ngang qua sân. Khi tiếp tục tham quan, hãy kiểm tra bản đồ để chọn địa điểm tiếp theo. Bạn có thể chuyển ngôn ngữ để đọc và nghe bài giới thiệu theo lựa chọn của mình.',10.7597778,106.6823611,20.00,1,'2026-10-25 00:34:44.058922','2026-10-25 01:38:14.713036'),(2,1,'SGU_SAN_BONG_DA','Sân bóng đá','Sân bóng đá trong khuôn viên Trường Đại học Sài Gòn, một điểm dừng trong hành trình khám phá trường.',10.7596667,106.6814444,20.00,1,'2026-10-25 01:37:28.459822','2026-10-25 01:37:28.459822');
/*!40000 ALTER TABLE `points_of_interest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tour_route_pois`
--

DROP TABLE IF EXISTS `tour_route_pois`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tour_route_pois` (
  `route_id` bigint unsigned NOT NULL,
  `poi_id` bigint unsigned NOT NULL,
  `sort_order` smallint unsigned NOT NULL,
  PRIMARY KEY (`route_id`,`poi_id`),
  UNIQUE KEY `uq_route_position` (`route_id`,`sort_order`),
  KEY `fk_routepoi_poi` (`poi_id`),
  CONSTRAINT `fk_routepoi_poi` FOREIGN KEY (`poi_id`) REFERENCES `points_of_interest` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_routepoi_route` FOREIGN KEY (`route_id`) REFERENCES `tour_routes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_routepoi_order` CHECK ((`sort_order` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tour_route_pois`
--

LOCK TABLES `tour_route_pois` WRITE;
/*!40000 ALTER TABLE `tour_route_pois` DISABLE KEYS */;
/*!40000 ALTER TABLE `tour_route_pois` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tour_routes`
--

DROP TABLE IF EXISTS `tour_routes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tour_routes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `route_code` varchar(50) NOT NULL,
  `route_name` varchar(200) NOT NULL,
  `description` text,
  `estimated_duration_minutes` smallint unsigned NOT NULL,
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_route_code` (`route_code`),
  CONSTRAINT `ck_route_code` CHECK ((char_length(trim(`route_code`)) > 0)),
  CONSTRAINT `ck_route_duration` CHECK ((`estimated_duration_minutes` > 0)),
  CONSTRAINT `ck_route_name` CHECK ((char_length(trim(`route_name`)) > 0)),
  CONSTRAINT `ck_route_visible` CHECK ((`is_visible` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tour_routes`
--

LOCK TABLES `tour_routes` WRITE;
/*!40000 ALTER TABLE `tour_routes` DISABLE KEYS */;
/*!40000 ALTER TABLE `tour_routes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visitor_sessions`
--

DROP TABLE IF EXISTS `visitor_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `visitor_sessions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `token_hash` binary(32) NOT NULL,
  `started_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `expires_at` datetime(6) NOT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `last_seen_at` datetime(6) DEFAULT NULL COMMENT 'Thoi diem UTC nhan heartbeat gan nhat',
  `is_listening` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1: dang phat audio; 0: dung hoac chua phat',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_visit_token` (`token_hash`),
  KEY `ix_visit_expiry` (`expires_at`),
  KEY `ix_visit_last_seen` (`last_seen_at`),
  CONSTRAINT `ck_visit_end` CHECK (((`ended_at` is null) or (`ended_at` >= `started_at`))),
  CONSTRAINT `ck_visit_expiry` CHECK ((`expires_at` > `started_at`))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visitor_sessions`
--

LOCK TABLES `visitor_sessions` WRITE;
/*!40000 ALTER TABLE `visitor_sessions` DISABLE KEYS */;
INSERT INTO `visitor_sessions` VALUES (1,_binary '~:I3)�Z�\�F���\�~\�gM�\�$h{��/D����','2026-10-24 15:15:37.656462','2026-10-24 15:45:48.505956','2026-10-24 15:15:49.227415','2026-10-24 15:15:48.505956',0),(2,_binary '\�2\�EO\�\�\0��b[�#)\�*\�B�w\0٥;\�\�j','2026-10-24 15:15:49.281926','2026-10-24 15:46:00.225591','2026-10-24 15:16:05.600472','2026-10-24 15:16:00.225591',0),(3,_binary '(�2cg�5\�c\rP�\�k\�i\�u��\�h��v\�0\�\��','2026-10-24 15:26:31.359759','2026-10-24 16:01:07.557590','2026-10-24 15:31:09.524993','2026-10-24 15:31:07.557590',0),(4,_binary '$\�\�R\�\�r\�<�\\\"5�9�ȵ|\�\�X\�\r_��','2026-10-24 15:31:09.544841','2026-10-24 16:01:13.760119','2026-10-24 15:31:15.644922','2026-10-24 15:31:13.760119',0),(5,_binary '\�\�\�M��G��D�\0��-�_��k����}�7t','2026-10-24 15:31:15.664889','2026-10-24 16:20:33.056615','2026-10-24 15:50:33.816275','2026-10-24 15:50:33.056615',0),(6,_binary '�i@G�7PX$\�\Zξ\�\�)\�%9�\�>7\�Q`{��','2026-10-24 15:50:33.848082','2026-10-24 16:20:33.859347','2026-10-24 15:50:34.564056','2026-10-24 15:50:33.859347',0),(7,_binary '\�*\�4\�+=]��i&�\�\�@\�m�\��a_e\�d\�J','2026-10-24 15:50:34.581924','2026-10-24 16:20:34.592743','2026-10-24 15:50:34.715588','2026-10-24 15:50:34.592743',0),(8,_binary '�z�t���u\�q}㥗n����\�A���jϹ\�','2026-10-24 15:50:34.738526','2026-10-24 16:20:34.748072','2026-10-24 15:50:34.875340','2026-10-24 15:50:34.748072',0),(9,_binary '/q<�*F\�\�\�\�\�����\�N\�X�̿�\�','2026-10-24 15:50:34.895480','2026-10-24 16:20:34.904354','2026-10-24 15:50:35.173794','2026-10-24 15:50:34.904354',0),(10,_binary '\�\��u.���{��\�Ǿ\�\�AI�\nK�9\��N/','2026-10-24 15:50:35.198270','2026-10-24 16:26:45.196725','2026-10-24 15:56:48.595630','2026-10-24 15:56:45.196725',0),(11,_binary '��ʞp\�S\�b\�\�X;=b��T\�(-W5�Z�$','2026-10-24 17:04:58.294774','2026-10-24 17:40:17.446528','2026-10-24 17:10:19.048235','2026-10-24 17:10:17.446528',0),(12,_binary 's�L:�\Z�H0InE��j��\�\�yb�\��\��u��\�','2026-10-24 17:10:19.074793','2026-10-24 17:43:59.066949','2026-10-24 17:14:06.219248','2026-10-24 17:13:59.066949',0);
/*!40000 ALTER TABLE `visitor_sessions` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-25  2:10:22
