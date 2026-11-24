-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: sgu_audio_guide
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.5

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
-- Current Database: `sgu_audio_guide`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sgu_audio_guide` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `sgu_audio_guide`;

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
INSERT INTO `admins` VALUES
(1,'admin','lethingocthaobd1234@gmail.com','$2a$12$IpzGlcBQT7ykJLxCH5.BRuvlx6zaEnMKCNIdmWP13J7Eqm1sOBD8G','Admin Demo',1,1,'2026-10-24 22:11:21.077628','2026-11-16 18:58:35.373456');
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
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audio_files`
--

LOCK TABLES `audio_files` WRITE;
/*!40000 ALTER TABLE `audio_files` DISABLE KEYS */;
INSERT INTO `audio_files` (`id`, `content_id`, `file_name`, `file_url`, `mime_type`, `file_size_bytes`, `duration_seconds`, `version_number`, `is_visible`, `created_at`, `updated_at`) VALUES
(1,1,'tts-1-vi.mp3','http://localhost:8084/api/audio/files/1.mp3','audio/mpeg',344736,28.73,1,0,'2026-10-25 00:54:01.622531','2026-10-25 01:39:39.000521'),
(2,2,'tts-2-en.mp3','http://localhost:8084/api/audio/files/2.mp3','audio/mpeg',353664,29.47,1,0,'2026-10-25 01:13:30.005088','2026-10-25 01:39:57.157342'),
(3,3,'tts-3-zh.mp3','http://localhost:8084/api/audio/files/3.mp3','audio/mpeg',360000,30.00,1,0,'2026-10-25 01:13:39.683412','2026-10-25 01:40:12.858444'),
(4,4,'tts-4-ko.mp3','http://localhost:8084/api/audio/files/4.mp3','audio/mpeg',363744,30.31,1,0,'2026-10-25 01:13:50.575466','2026-10-25 01:40:27.716581'),
(5,1,'tts-1-vi.mp3','http://localhost:8084/api/audio/files/5.mp3','audio/mpeg',310176,25.85,2,0,'2026-10-25 01:39:39.009535','2026-10-27 14:34:15.737617'),
(6,2,'tts-2-en.mp3','http://localhost:8084/api/audio/files/6.mp3','audio/mpeg',357696,29.81,2,0,'2026-10-25 01:39:57.158791','2026-10-27 14:34:27.234493'),
(7,3,'tts-3-zh.mp3','http://localhost:8084/api/audio/files/7.mp3','audio/mpeg',347616,28.97,2,0,'2026-10-25 01:40:12.862652','2026-10-27 14:34:39.185852'),
(8,4,'tts-4-ko.mp3','http://localhost:8084/api/audio/files/8.mp3','audio/mpeg',356256,29.69,2,0,'2026-10-25 01:40:27.718018','2026-10-27 14:34:51.629908'),
(9,6,'tts-6-vi.mp3','http://localhost:8084/api/audio/files/9.mp3','audio/mpeg',325440,27.12,1,1,'2026-10-25 01:45:41.187960','2026-10-25 01:45:41.189689'),
(10,7,'tts-7-en.mp3','http://localhost:8084/api/audio/files/10.mp3','audio/mpeg',347040,28.92,1,1,'2026-10-25 01:45:53.123226','2026-10-25 01:45:53.125496'),
(11,8,'tts-8-zh.mp3','http://localhost:8084/api/audio/files/11.mp3','audio/mpeg',346176,28.85,1,1,'2026-10-25 01:55:15.770029','2026-10-25 01:55:15.771199'),
(12,9,'tts-9-ko.mp3','http://localhost:8084/api/audio/files/12.mp3','audio/mpeg',349056,29.09,1,1,'2026-10-25 02:01:25.970080','2026-10-25 02:01:25.973223'),
(13,10,'tts-10-vi.mp3','http://localhost:8084/api/audio/files/13.mp3','audio/mpeg',147744,12.31,1,1,'2026-10-27 14:32:48.696196','2026-10-27 14:32:48.706192'),
(14,11,'tts-11-en.mp3','http://localhost:8084/api/audio/files/14.mp3','audio/mpeg',157536,13.13,1,1,'2026-10-27 14:32:57.349763','2026-10-27 14:32:57.351077'),
(15,12,'tts-12-zh.mp3','http://localhost:8084/api/audio/files/15.mp3','audio/mpeg',166176,13.85,1,1,'2026-10-27 14:33:06.054381','2026-10-27 14:33:06.055753'),
(16,13,'tts-13-ko.mp3','http://localhost:8084/api/audio/files/16.mp3','audio/mpeg',167904,13.99,1,1,'2026-10-27 14:33:14.357914','2026-10-27 14:33:14.361622'),
(17,1,'tts-1-vi.mp3','http://localhost:8084/api/audio/files/17.mp3','audio/mpeg',317376,26.45,3,1,'2026-10-27 14:34:15.739580','2026-10-27 14:34:15.740726'),
(18,2,'tts-2-en.mp3','http://localhost:8084/api/audio/files/18.mp3','audio/mpeg',350496,29.21,3,1,'2026-10-27 14:34:27.236059','2026-10-27 14:34:27.237278'),
(19,3,'tts-3-zh.mp3','http://localhost:8084/api/audio/files/19.mp3','audio/mpeg',349056,29.09,3,1,'2026-10-27 14:34:39.189199','2026-10-27 14:34:39.191103'),
(20,4,'tts-4-ko.mp3','http://localhost:8084/api/audio/files/20.mp3','audio/mpeg',379296,31.61,3,1,'2026-10-27 14:34:51.631075','2026-10-27 14:34:51.632285'),
(21,14,'tts-14-vi.mp3','http://localhost:8084/api/audio/files/21.mp3','audio/mpeg',134784,11.23,1,1,'2026-10-27 14:37:48.973761','2026-10-27 14:37:48.974829'),
(22,17,'tts-17-en.mp3','http://localhost:8084/api/audio/files/22.mp3','audio/mpeg',169344,14.11,1,1,'2026-10-27 14:37:59.266488','2026-10-27 14:37:59.267852'),
(23,18,'tts-18-zh.mp3','http://localhost:8084/api/audio/files/23.mp3','audio/mpeg',169344,14.11,1,1,'2026-10-27 14:38:08.249698','2026-10-27 14:38:08.250651'),
(24,19,'tts-19-ko.mp3','http://localhost:8084/api/audio/files/24.mp3','audio/mpeg',174816,14.57,1,1,'2026-10-27 14:38:18.087572','2026-10-27 14:38:18.088524'),
(25,20,'tts-20-vi.mp3','http://localhost:8084/api/audio/files/25.mp3','audio/mpeg',22464,1.87,1,0,'2026-10-28 07:21:08.861336','2026-10-28 07:22:36.977874'),
(26,21,'tts-21-en.mp3','http://localhost:8084/api/audio/files/26.mp3','audio/mpeg',14400,1.20,1,0,'2026-10-28 07:21:15.435311','2026-10-28 07:22:43.659525'),
(27,22,'tts-22-zh.mp3','http://localhost:8084/api/audio/files/27.mp3','audio/mpeg',28224,2.35,1,0,'2026-10-28 07:21:22.215436','2026-10-28 07:22:50.570500'),
(28,23,'tts-23-ko.mp3','http://localhost:8084/api/audio/files/28.mp3','audio/mpeg',19296,1.61,1,0,'2026-10-28 07:21:28.633204','2026-10-28 07:22:57.593309'),
(29,20,'tts-20-vi.mp3','http://localhost:8084/api/audio/files/29.mp3','audio/mpeg',40896,3.41,2,0,'2026-10-28 07:22:36.980286','2026-10-28 07:28:53.528078'),
(30,21,'tts-21-en.mp3','http://localhost:8084/api/audio/files/30.mp3','audio/mpeg',46080,3.84,2,0,'2026-10-28 07:22:43.660968','2026-10-28 07:29:03.722023'),
(31,22,'tts-22-zh.mp3','http://localhost:8084/api/audio/files/31.mp3','audio/mpeg',40320,3.36,2,0,'2026-10-28 07:22:50.571766','2026-10-28 07:29:13.552761'),
(32,23,'tts-23-ko.mp3','http://localhost:8084/api/audio/files/32.mp3','audio/mpeg',43200,3.60,2,0,'2026-10-28 07:22:57.595308','2026-11-06 22:48:04.671745'),
(33,20,'tts-20-vi.mp3','http://localhost:8084/api/audio/files/33.mp3','audio/mpeg',296064,24.67,3,0,'2026-10-28 07:28:53.531434','2026-10-30 00:32:34.640318'),
(34,21,'tts-21-en.mp3','http://localhost:8084/api/audio/files/34.mp3','audio/mpeg',302976,25.25,3,0,'2026-10-28 07:29:03.723474','2026-10-30 00:32:44.538560'),
(35,22,'tts-22-zh.mp3','http://localhost:8084/api/audio/files/35.mp3','audio/mpeg',291744,24.31,3,0,'2026-10-28 07:29:13.554099','2026-10-28 14:43:29.063509'),
(36,24,'tts-24-vi.mp3','http://localhost:8084/api/audio/files/36.mp3','audio/mpeg',285696,23.81,1,0,'2026-10-28 07:37:48.830074','2026-10-28 07:44:17.855723'),
(37,25,'tts-25-en.mp3','http://localhost:8084/api/audio/files/37.mp3','audio/mpeg',284544,23.71,1,0,'2026-10-28 07:42:03.382306','2026-10-28 14:40:00.908847'),
(38,24,'tts-24-vi.mp3','http://localhost:8084/api/audio/files/38.mp3','audio/mpeg',275040,22.92,2,0,'2026-10-28 07:44:17.857122','2026-10-28 14:39:32.211938'),
(39,30,'tts-30-zh.mp3','http://localhost:8084/api/audio/files/39.mp3','audio/mpeg',288576,24.05,1,0,'2026-10-28 08:00:38.996822','2026-10-28 14:38:32.064242'),
(40,28,'tts-28-vi.mp3','http://localhost:8084/api/audio/files/40.mp3','audio/mpeg',249120,20.76,1,0,'2026-10-28 14:37:37.425620','2026-10-30 00:29:46.833906'),
(41,30,'tts-30-zh.mp3','http://localhost:8084/api/audio/files/41.mp3','audio/mpeg',268704,22.39,2,0,'2026-10-28 14:38:32.066236','2026-10-30 00:30:07.179603'),
(42,24,'tts-24-vi.mp3','http://localhost:8084/api/audio/files/42.mp3','audio/mpeg',275616,22.97,3,0,'2026-10-28 14:39:32.213544','2026-10-30 00:31:12.753313'),
(43,25,'tts-25-en.mp3','http://localhost:8084/api/audio/files/43.mp3','audio/mpeg',280224,23.35,2,0,'2026-10-28 14:40:00.909912','2026-10-30 00:31:22.745101'),
(44,22,'tts-22-zh.mp3','http://localhost:8084/api/audio/files/44.mp3','audio/mpeg',302400,25.20,4,0,'2026-10-28 14:43:29.064708','2026-11-06 22:47:53.598835'),
(45,28,'tts-28-vi.mp3','http://localhost:8084/api/audio/files/45.mp3','audio/mpeg',247104,20.59,2,1,'2026-10-30 00:29:46.850169','2026-10-30 00:29:46.860770'),
(46,29,'tts-29-en.mp3','http://localhost:8084/api/audio/files/46.mp3','audio/mpeg',278496,23.21,1,1,'2026-10-30 00:29:57.259982','2026-10-30 00:29:57.262521'),
(47,30,'tts-30-zh.mp3','http://localhost:8084/api/audio/files/47.mp3','audio/mpeg',257184,21.43,3,1,'2026-10-30 00:30:07.181181','2026-10-30 00:30:07.184590'),
(48,31,'tts-31-ko.mp3','http://localhost:8084/api/audio/files/48.mp3','audio/mpeg',262944,21.91,1,1,'2026-10-30 00:30:17.792689','2026-10-30 00:30:17.795748'),
(49,24,'tts-24-vi.mp3','http://localhost:8084/api/audio/files/49.mp3','audio/mpeg',269280,22.44,4,1,'2026-10-30 00:31:12.754370','2026-10-30 00:31:12.756019'),
(50,25,'tts-25-en.mp3','http://localhost:8084/api/audio/files/50.mp3','audio/mpeg',298080,24.84,3,1,'2026-10-30 00:31:22.746309','2026-10-30 00:31:22.748365'),
(51,26,'tts-26-zh.mp3','http://localhost:8084/api/audio/files/51.mp3','audio/mpeg',290016,24.17,1,1,'2026-10-30 00:31:32.290111','2026-10-30 00:31:32.291854'),
(52,27,'tts-27-ko.mp3','http://localhost:8084/api/audio/files/52.mp3','audio/mpeg',340704,28.39,1,1,'2026-10-30 00:31:43.211905','2026-10-30 00:31:43.214457'),
(53,20,'tts-20-vi.mp3','http://localhost:8084/api/audio/files/53.mp3','audio/mpeg',271584,22.63,4,0,'2026-10-30 00:32:34.641619','2026-11-06 22:47:32.937443'),
(54,21,'tts-21-en.mp3','http://localhost:8084/api/audio/files/54.mp3','audio/mpeg',308736,25.73,4,0,'2026-10-30 00:32:44.540411','2026-11-06 22:47:44.122779'),
(55,36,'tts-36-zh.mp3','http://localhost:8084/api/audio/files/55.mp3','audio/mpeg',253440,21.12,1,1,'2026-10-30 00:47:44.843233','2026-10-30 00:47:44.854715'),
(56,37,'tts-37-ko.mp3','http://localhost:8084/api/audio/files/56.mp3','audio/mpeg',258336,21.53,1,1,'2026-10-30 00:47:54.356481','2026-10-30 00:47:54.357687'),
(57,34,'tts-34-vi.mp3','http://localhost:8084/api/audio/files/57.mp3','audio/mpeg',234144,19.51,1,1,'2026-10-30 00:48:26.932706','2026-10-30 00:48:26.934457'),
(58,35,'tts-35-en.mp3','http://localhost:8084/api/audio/files/58.mp3','audio/mpeg',319104,26.59,1,1,'2026-10-30 00:48:37.288733','2026-10-30 00:48:37.289817'),
(59,38,'tts-38-vi.mp3','http://localhost:8084/api/audio/files/59.mp3','audio/mpeg',227520,18.96,1,1,'2026-10-30 00:51:20.664534','2026-10-30 00:51:20.666289'),
(60,39,'tts-39-en.mp3','http://localhost:8084/api/audio/files/60.mp3','audio/mpeg',259200,21.60,1,1,'2026-10-30 00:51:30.478067','2026-10-30 00:51:30.479180'),
(61,40,'tts-40-zh.mp3','http://localhost:8084/api/audio/files/61.mp3','audio/mpeg',268704,22.39,1,1,'2026-10-30 00:51:39.236356','2026-10-30 00:51:39.238206'),
(62,44,'tts-44-zh.mp3','http://localhost:8084/api/audio/files/62.mp3','audio/mpeg',324000,27.00,1,0,'2026-10-30 00:57:10.121738','2026-11-16 12:07:06.570593'),
(63,20,'tts-20-vi.mp3','http://localhost:8084/api/audio/files/63.mp3','audio/mpeg',222336,18.53,5,1,'2026-11-06 22:47:32.957446','2026-11-06 22:47:32.970454'),
(64,21,'tts-21-en.mp3','http://localhost:8084/api/audio/files/64.mp3','audio/mpeg',278784,23.23,5,1,'2026-11-06 22:47:44.126151','2026-11-06 22:47:44.127351'),
(65,22,'tts-22-zh.mp3','http://localhost:8084/api/audio/files/65.mp3','audio/mpeg',220896,18.41,5,1,'2026-11-06 22:47:53.600855','2026-11-06 22:47:53.602144'),
(66,23,'tts-23-ko.mp3','http://localhost:8084/api/audio/files/66.mp3','audio/mpeg',267840,22.32,3,1,'2026-11-06 22:48:04.673172','2026-11-06 22:48:04.675149'),
(67,46,'tts-46-vi.mp3','http://localhost:8084/api/audio/files/67.mp3','audio/mpeg',81504,6.79,1,1,'2026-11-16 10:00:07.928353','2026-11-16 10:00:07.942923'),
(68,47,'tts-47-en.mp3','http://localhost:8084/api/audio/files/68.mp3','audio/mpeg',111456,9.29,1,1,'2026-11-16 10:00:17.575143','2026-11-16 10:00:17.578353'),
(69,48,'tts-48-zh.mp3','http://localhost:8084/api/audio/files/69.mp3','audio/mpeg',89856,7.49,1,1,'2026-11-16 10:00:26.110185','2026-11-16 10:00:26.112136'),
(70,49,'tts-49-ko.mp3','http://localhost:8084/api/audio/files/70.mp3','audio/mpeg',114336,9.53,1,1,'2026-11-16 10:00:35.717657','2026-11-16 10:00:35.720094'),
(71,42,'tts-42-vi.mp3','http://localhost:8084/api/audio/files/71.mp3','audio/mpeg',257760,21.48,1,1,'2026-11-16 12:06:41.377798','2026-11-16 12:06:41.388271'),
(72,43,'tts-43-en.mp3','http://localhost:8084/api/audio/files/72.mp3','audio/mpeg',330624,27.55,1,1,'2026-11-16 12:06:54.210856','2026-11-16 12:06:54.213110'),
(73,44,'tts-44-zh.mp3','http://localhost:8084/api/audio/files/73.mp3','audio/mpeg',339840,28.32,2,1,'2026-11-16 12:07:06.575438','2026-11-16 12:07:06.576918'),
(74,45,'tts-45-ko.mp3','http://localhost:8084/api/audio/files/74.mp3','audio/mpeg',318240,26.52,1,1,'2026-11-16 12:07:17.188712','2026-11-16 12:07:17.191476');
/*!40000 ALTER TABLE `audio_files` ENABLE KEYS */;
UNLOCK TABLES;

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
INSERT INTO `campus_areas` VALUES
(1,'SGU_MAIN','Khuôn viên Trường Đại học Sài Gòn','Khu vực quản lý các địa điểm thuyết minh của trường.',1,'2026-10-25 00:09:53.161336','2026-10-25 00:09:53.161336');
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `languages`
--

LOCK TABLES `languages` WRITE;
/*!40000 ALTER TABLE `languages` DISABLE KEYS */;
INSERT INTO `languages` VALUES
(1,'vi','Tiếng Việt',1),
(2,'en','Tiếng Anh',1),
(3,'zh','Tiếng Trung',1),
(4,'ko','Tiếng Hàn',1);
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
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `listening_logs`
--

LOCK TABLES `listening_logs` WRITE;
/*!40000 ALTER TABLE `listening_logs` DISABLE KEYS */;
INSERT INTO `listening_logs` VALUES
(1,'2345130d-9933-4359-9a73-bdd0de4a8651',13,9,'SIMULATION','2026-10-25 14:34:16.314884'),
(2,'2337a15d-e965-49e3-bc52-822e4d2c949e',54,5,'SIMULATION','2026-10-26 18:21:59.072192'),
(3,'4e19a1ed-aa17-4f13-9786-e203e4197be0',54,5,'SIMULATION','2026-10-26 18:24:49.978625'),
(4,'eefa09e4-5784-46d6-95f0-ef62a263bd9d',57,5,'SIMULATION','2026-10-26 18:56:39.051509'),
(5,'31236d35-9e8a-4d97-ac5e-4e392c90b7cd',159,17,'SIMULATION','2026-10-30 02:03:14.199491'),
(6,'b83a37f5-0c05-431e-9920-8f5ef4a2e03f',168,45,'SIMULATION','2026-10-30 02:59:37.309368'),
(7,'90add900-6e45-40e5-9d42-7588a6397716',167,46,'SIMULATION','2026-10-30 02:59:45.085583'),
(8,'da2780f5-684b-423d-8e5a-4dccc184ad89',167,46,'SIMULATION','2026-10-30 03:00:28.865783'),
(9,'854f7764-9bba-4a3c-89db-3422ce6a284d',167,47,'SIMULATION','2026-10-30 03:00:34.875414'),
(10,'1040b31c-5747-4df4-8571-21d52dff46ee',190,45,'SIMULATION','2026-11-05 07:11:14.658539'),
(11,'e96cbade-cb22-40cf-b63d-559c0a1d3122',190,46,'SIMULATION','2026-11-05 07:11:45.058475'),
(12,'24f9f0d8-1658-4e10-9da2-6be638b425f9',190,47,'SIMULATION','2026-11-05 07:11:54.617888'),
(13,'6946751b-1a33-4d0e-9b5c-a75b3c1908da',200,45,'SIMULATION','2026-11-05 14:07:47.682390'),
(14,'181f55d0-3a54-441b-8a5b-23649085c788',200,48,'SIMULATION','2026-11-05 14:08:00.181645'),
(15,'918bb98e-8ee9-4393-b7e1-27390c453844',203,62,'SIMULATION','2026-11-05 14:09:09.858032'),
(16,'23a072f5-28e7-46f5-b002-0272c31dd485',203,62,'SIMULATION','2026-11-05 14:09:25.060548'),
(17,'2fa2a749-35da-4112-aacb-d33ff12546cc',263,59,'SIMULATION','2026-11-14 16:02:48.170391'),
(18,'f52584e6-10d2-4fcb-a0f4-2e05e482e041',263,60,'SIMULATION','2026-11-14 16:03:14.254977'),
(19,'947ac61b-fd12-44d9-b375-82d7610362fa',286,17,'SIMULATION','2026-11-16 11:48:10.642806'),
(20,'57838fed-48f6-4674-8658-096d3fb9c86e',286,67,'SIMULATION','2026-11-16 11:50:34.288636'),
(21,'835bca1e-5ab2-4414-a548-db91b34b8dfd',286,67,'SIMULATION','2026-11-16 11:50:43.778335'),
(22,'41c3bbf4-70cd-4363-810c-d0ccec54b349',286,45,'SIMULATION','2026-11-16 11:55:25.339719'),
(23,'d3aa46f4-fd3b-46c7-94a7-9ba6b15b0ff2',286,17,'SIMULATION','2026-11-16 12:02:58.252628'),
(24,'37468cf6-1a63-4378-8f67-77d479b320fa',286,71,'SIMULATION','2026-11-16 12:07:42.532235'),
(25,'2548de22-ad19-44fa-acb0-0a914036ace2',288,67,'SIMULATION','2026-11-16 12:26:11.377267'),
(26,'ce4b3e83-fe67-4294-8a72-e043f8eff7d7',307,67,'SIMULATION','2026-11-17 02:41:49.792794'),
(27,'bda32228-080f-4378-80be-5077ab7a053a',317,21,'SIMULATION','2026-11-17 04:04:21.850475'),
(28,'13e6372d-e3a2-4810-8fce-279a834b6155',334,45,'SIMULATION','2026-11-18 12:18:02.725460');
/*!40000 ALTER TABLE `listening_logs` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `narration_contents`
--

LOCK TABLES `narration_contents` WRITE;
/*!40000 ALTER TABLE `narration_contents` DISABLE KEYS */;
INSERT INTO `narration_contents` VALUES
(1,1,1,'Khám phá sân bóng chuyền SGU','Chào mừng bạn đến với sân bóng chuyền trong khuôn viên Trường Đại học Sài Gòn. Tại điểm dừng này, hãy dành một chút thời gian quan sát sân và không gian xung quanh. Nếu đang có trận đấu hoặc buổi tập, bạn hãy đứng ngoài đường biên, giữ khoảng cách an toàn và tránh đi ngang qua sân. Khi tiếp tục tham quan, hãy kiểm tra bản đồ để chọn địa điểm tiếp theo. Bạn có thể chuyển ngôn ngữ để đọc và nghe bài giới thiệu theo lựa chọn của mình.',1,'2026-10-25 00:37:36.041959','2026-10-27 14:34:01.460389'),
(2,1,2,'Discovering the SGU Volleyball Court','Welcome to the volleyball court on the campus of Saigon University. At this stop, take a moment to observe the court and the surrounding space. If a match or training session is taking place, please stand outside the boundary line, maintain a safe distance, and avoid walking across the court. As you continue your tour, check the map to choose your next destination. You can change the language to read and listen to the introduction according to your preference.',1,'2026-10-25 01:13:13.164874','2026-10-27 14:34:18.118605'),
(3,1,3,'探索西贡大学排球场','欢迎来到西贡大学校园内的排球场。在这个站点，请花一点时间观察球场及周围的环境。如果正在进行比赛或训练，请站在边线外，保持安全距离并避免横穿球场。继续参观时，请查看地图以选择下一个地点。您可以切换语言，根据您的喜好阅读和收听介绍。',1,'2026-10-25 01:13:31.235611','2026-10-27 14:34:28.780842'),
(4,1,4,'SGU 배구장 탐방','사이공 대학교 캠퍼스 내 배구장에 오신 것을 환영합니다. 이 장소에서는 잠시 시간을 내어 경기장과 주변 공간을 살펴보세요. 경기나 훈련이 진행 중인 경우, 경기장 밖 라인에 서서 안전거리를 유지하고 경기장을 가로질러 가지 않도록 주의해 주세요. 투어를 계속하려면 지도를 확인하여 다음 장소를 선택하세요. 본인의 선호에 따라 언어를 전환하여 소개 글을 읽고 들을 수 있습니다.',1,'2026-10-25 01:13:41.379891','2026-10-27 14:34:41.947875'),
(6,2,1,'Khám phá sân bóng đá SGU','Chào mừng bạn đến với sân bóng đá trong khuôn viên Trường Đại học Sài Gòn. Tại điểm dừng này, hãy dành một chút thời gian quan sát sân và không gian xung quanh. Nếu đang có trận đấu hoặc buổi tập, bạn hãy đứng ngoài đường biên, giữ khoảng cách an toàn và tránh đi ngang qua sân. Khi tiếp tục tham quan, hãy kiểm tra bản đồ để chọn địa điểm tiếp theo. Bạn có thể chuyển ngôn ngữ để đọc và nghe bài giới thiệu theo lựa chọn của mình.',1,'2026-10-25 01:45:12.679537','2026-10-25 01:45:12.679537'),
(7,2,2,'Explore the SGU Football Field','Welcome to the football field on the campus of Saigon University. At this stop, take a moment to observe the field and the surrounding space. If a match or practice session is taking place, please stand outside the sideline, keep a safe distance, and avoid walking across the field. When you continue your tour, check the map to choose your next destination. You can change the language to read and listen to the introduction according to your preference.',1,'2026-10-25 01:45:44.844300','2026-10-25 01:45:44.844300'),
(8,2,3,'探索西贡大学足球场','欢迎来到西贡大学校园内的足球场。在此停留点，请花点时间观察球场及周围空间。如果正在进行比赛或训练，请站在边线外，保持安全距离并避免横穿球场。继续参观时，请查看地图以选择下一个地点。您可以切换语言，根据您的喜好阅读和收听介绍。',1,'2026-10-25 01:45:54.388873','2026-10-25 01:45:54.388873'),
(9,2,4,'SGU 축구장 탐방','사이공 대학교 캠퍼스 내 축구장에 오신 것을 환영합니다. 이 장소에서 잠시 멈추어 경기장과 주변 공간을 둘러보세요. 경기나 훈련이 진행 중인 경우, 터치라인 밖에 서서 안전거리를 유지하고 경기장을 가로질러 가지 않도록 하세요. 관람을 계속하려면 지도를 확인하여 다음 장소를 선택하세요. 원하는 언어로 소개를 읽고 들으려면 언어를 변경할 수 있습니다.',1,'2026-10-25 01:45:59.323472','2026-10-25 01:45:59.323472'),
(10,3,1,'Khám phá Tòa B','Tòa B là một điểm dừng trong hành trình khám phá khuôn viên Trường Đại học Sài Gòn. Khi đến đây, bạn có thể quan sát không gian xung quanh và kiểm tra bản đồ để tìm địa điểm tiếp theo.',1,'2026-10-27 14:15:15.308195','2026-10-27 14:15:15.308195'),
(11,3,2,'Exploring Building B','Building B is a stop on the journey to explore the Saigon University campus. When you arrive here, you can observe the surrounding space and check the map to find your next destination.',1,'2026-10-27 14:32:50.230905','2026-10-27 14:32:50.230905'),
(12,3,3,'探索B栋','B栋是探索西贡大学校园之旅的一个经停点。当你来到这里时，你可以观察周围的空间并查看地图以寻找下一个目的地。',1,'2026-10-27 14:32:58.433298','2026-10-27 14:32:58.433298'),
(13,3,4,'B동 탐방','B동은 사이공대학교 캠퍼스 탐방 여정의 정류장 중 하나입니다. 이곳에 도착하시면 주변 공간을 둘러보고 지도를 확인하여 다음 장소를 찾으실 수 있습니다.',1,'2026-10-27 14:33:07.231705','2026-10-27 14:33:07.231705'),
(14,4,1,'Khu A','Tòa A nằm trong khuôn viên Trường Đại học Sài Gòn. Khi tham quan khu vực này, bạn hãy di chuyển theo lối đi chung, giữ trật tự và tránh làm ảnh hưởng đến hoạt động của nhà trường.',1,'2026-10-27 14:35:55.926159','2026-10-27 14:35:55.926159'),
(17,4,2,'Area A','Building A is located within the campus of Saigon University. When visiting this area, please move along the common pathways, maintain order, and avoid disrupting the activities of the school.',1,'2026-10-27 14:37:52.174948','2026-10-27 14:37:52.174948'),
(18,4,3,'A区','A栋位于西贡大学校园内。参观该区域时，请按公共通道行走，保持安静，避免影响学校的各项活动。',1,'2026-10-27 14:38:01.413737','2026-10-27 14:38:01.413737'),
(19,4,4,'A 구역','A동은 사이공 대학교 캠퍼스 내에 위치해 있습니다. 이 구역을 관람하실 때는 공용 통로로 이동하시고, 질서를 지키며 학교 활동에 지장을 주지 않도록 주의해 주시기 바랍니다.',1,'2026-10-27 14:38:11.308060','2026-10-27 14:38:11.308060'),
(20,6,1,'Khám phá Tòa C','Tòa C nằm trong khuôn viên Trường Đại học Sài Gòn. Tòa nhà mang nét kiến trúc cổ kính, kết hợp giữa phong cách phương Tây và Trung Hoa. Khi tham quan khu vực này, bạn hãy di chuyển theo lối đi chung, giữ trật tự và tránh làm ảnh hưởng đến hoạt động học tập của nhà trường.',1,'2026-10-28 07:20:43.842338','2026-11-06 22:47:13.838922'),
(21,6,2,'Exploring Building C','Building C is located within the campus of Saigon University. The building features an ancient architectural style, combining Western and Chinese influences. When visiting this area, please walk along the common pathways, maintain quietness, and avoid disrupting the university\'s learning activities.',1,'2026-10-28 07:21:10.582094','2026-11-06 22:47:34.482171'),
(22,6,3,'探索C栋','C栋位于西贡大学校园内。该建筑具有古朴的建筑风格，融合了西方与中国风情。参观该区域时，请按公共通道行走，保持安静，避免影响学校的教学活动。',1,'2026-10-28 07:21:16.336321','2026-11-06 22:47:45.114496'),
(23,6,4,'C관 탐방','C관은 사이공 대학교 캠퍼스 내에 위치하고 있습니다. 이 건물은 서양식과 중화 양식이 결합된 고풍스러운 건축 양식을 지니고 있습니다. 이 구역을 관람하실 때는 공용 통로로 이동하시고, 질서를 지키며 학교의 학습 활동에 지장을 주지 않도록 주의해 주시기 바랍니다.',1,'2026-10-28 07:21:23.287135','2026-11-06 22:47:54.744978'),
(24,5,1,'Khám phá Toà E','Tòa E, toà nhà này nằm ngay khu vực cổng vào Nguyễn Trãi của Trường Đại học Sài Gòn, nằm gần khuôn viên cơ sở chính của nhà trường. Đây là khu vực gắn liền với các hoạt động sinh hoạt thường ngày. Khi tham quan khu vực này, bạn hãy di chuyển theo lối đi chung, giữ trật tự, không tự ý đi vào các không gian riêng và tránh làm ảnh hưởng đến sinh hoạt của những người xung quanh.',1,'2026-10-28 07:34:54.354505','2026-10-28 07:34:54.354505'),
(25,5,2,'Exploring Building E','Building E is located right at the Nguyen Trai entrance area of Saigon University, near the main campus grounds. This area is associated with daily activities. When visiting this area, please move along the common pathways, keep quiet, do not enter private spaces without permission, and avoid disturbing the activities of those around you.',1,'2026-10-28 07:35:02.050896','2026-10-30 00:31:14.119165'),
(26,5,3,'探索E栋','E栋，这栋建筑位于西贡大学阮ãi大门区域附近，靠近学校主校区。这里是与日常生活活动紧密相关的区域。参观该区域时，请按公共通道行走，保持安静，不要擅自进入私人空间，避免打扰周围人员的日常生活。',1,'2026-10-28 07:35:03.601848','2026-10-30 00:31:23.777076'),
(27,5,4,'E동 탐방','E동은 사이공 대학교의 응우옌짜이(Nguyễn Trãi) 정문 구역에 바로 위치해 있으며, 대학 본교 캠퍼스와 가깝습니다. 이곳은 일상적인 활동과 밀접하게 관련된 구역입니다. 이 구역을 관람하실 때는 공용 통로로 이동하시고, 질서를 지키며, 임의로 사적 공간에 들어가지 마시고 주변 사람들의 생활에 방해가 되지 않도록 주의해 주시기 바랍니다.',1,'2026-10-28 07:35:05.618971','2026-10-30 00:31:34.142069'),
(28,9,1,'Khám phá Cổng chính','Cổng chính Trường Đại học Sài Gòn nằm tại số 273 đường An Dương Vương, Thành phố Hồ Chí Minh. Đây là lối vào chính cơ sở chính của trường Đại học Sài Gòn. Khi di chuyển qua khu vực cổng, bạn hãy đi đúng lối quy định, giữ trật tự, chú ý an toàn giao thông và tuân thủ hướng dẫn của nhân viên bảo vệ.',1,'2026-10-28 08:00:18.320101','2026-10-28 08:07:06.776472'),
(29,9,2,'Exploring the Main Gate','The Main Gate of Saigon University is located at 273 An Duong Vuong Street, Ho Chi Minh City. This is the main entrance to the main campus of Saigon University. When passing through the gate area, please use the designated lanes, keep order, pay attention to traffic safety, and follow the instructions of the security staff.',1,'2026-10-28 08:00:26.085053','2026-10-30 00:29:48.301284'),
(30,9,3,'探索正门','西贡大学正门位于胡志明市安阳王街273号。这是西贡大学主校区的主要入口。经过门禁区域时，请按照规定路线行走，保持安静，注意交通安全并服从保安人员的指引。',1,'2026-10-28 08:00:28.116898','2026-10-30 00:29:58.438680'),
(31,9,4,'정문 탐방','사이공 대학교의 정문은 호치민시 안즈엉브엉가 273번지에 위치해 있습니다. 이곳은 사이공 대학교 본 캠퍼스의 주출입구입니다. 문을 통과해 이동할 때에는 지정된 통로로 다큐고, 질서를 지키며, 교통안전에 유의하고 경비원의 안내에 따라 주시기 바랍니다.',1,'2026-10-28 08:00:42.408027','2026-10-30 00:30:08.648310'),
(34,13,1,'Khám phá Cổng Nguyễn Trãi','Cổng phụ Nguyễn Trãi nằm trên đường Nguyễn Trãi, là một trong những lối ra vào của Trường Đại học Sài Gòn. Khu vực này là lối vào chính của bãi gửi xe trường. Khi đi qua cổng, bạn hãy đi đúng lối quy định, giữ trật tự, chú ý an toàn giao thông và tuân thủ hướng dẫn của nhân viên bảo vệ.',1,'2026-10-30 00:37:32.696341','2026-10-30 00:37:32.696341'),
(35,13,2,'Explore Nguyen Trai Gate','The Nguyen Trai auxiliary gate is located on Nguyen Trai street, serving as one of the entrances and exits of Saigon University. This area is the main entrance to the university\'s parking lot. When passing through the gate, please use the designated lanes, maintain order, pay attention to traffic safety, and follow the instructions of the security staff.',1,'2026-10-30 00:37:47.044259','2026-10-30 00:48:28.134553'),
(36,13,3,'探索阮廌门','阮廌侧门位于阮廌街，是西贡大学的出入口之一。该区域是学校停车场的主入口。穿过大门时，请走规定路线，保持秩序，注意交通安全并遵守保安人员的指引。',1,'2026-10-30 00:37:48.521464','2026-10-30 00:48:38.228790'),
(37,13,4,'응우옌짜이 문 탐험','응우옌짜이 부속문은 응우옌짜이 도로에 위치하며 사이공 대학교의 출입구 중 하나입니다. 이 구역은 교내 주차장으로 통하는 주출입구입니다. 문을 지날 때는 지정된 통행로를 이용하고, 질서를 지키며, 교통안전에 유의하고 보안 직원의 안내를 따라 주시기 바랍니다.',1,'2026-10-30 00:37:50.317876','2026-10-30 00:48:39.784536'),
(38,16,1,'Khám phá Bãi gửi xe','Bãi gửi xe nằm trong khuôn viên Trường Đại học Sài Gòn, là khu vực phục vụ nhu cầu gửi phương tiện của sinh viên, giảng viên và khách đến trường. Khi di chuyển trong khu vực này, bạn hãy đi chậm, đỗ xe đúng vị trí quy định, giữ gìn tài sản cá nhân và tuân thủ hướng dẫn của nhân viên trông giữ xe.',1,'2026-10-30 00:51:06.704332','2026-10-30 00:51:06.704332'),
(39,16,2,'Exploring the Parking Lot','The parking lot is located within the campus of Saigon University, serving as an area for students, faculty, and visitors to park their vehicles. When moving around this area, please drive slowly, park in the designated spots, keep your personal belongings secure, and follow the instructions of the parking attendants.',1,'2026-10-30 00:51:21.700323','2026-10-30 00:51:21.700323'),
(40,16,3,'探索停车场','停车场位于西贡大学校园内，是为学生、教师和来校访客提供车辆停放服务的区域。在此区域内行驶时，请减速慢行，按规定位置停放车辆，妥善保管个人财物，并服从停车场管理人员的指挥。',1,'2026-10-30 00:51:31.422533','2026-10-30 00:51:31.422533'),
(41,16,4,'주차장 탐방','주차장은 사이공 대학교 캠퍼스 내에 위치해 있으며, 학생, 교직원, 그리고 학교 방문객들의 차량 주차 수요를 충족시키는 구역입니다. 이 구역 내에서 이동할 때에는 서행하고, 지정된 위치에 주차하며, 개인 소지품을 잘 보관하고 주차 관리 직원의 안내를 따르시기 바랍니다.',1,'2026-10-30 00:51:40.259505','2026-10-30 00:51:40.259505'),
(42,8,1,'Khám phá KLF','KLF là khu tự học thuộc Trung tâm Học liệu Trường Đại học Sài Gòn, phục vụ nhu cầu học tập và nghiên cứu của sinh viên. Đây là nơi sinh viên có thể tự học, đọc tài liệu và học nhóm trong không gian yên tĩnh. Khi sử dụng khu vực này, bạn hãy giữ trật tự, hạn chế gây tiếng ồn, giữ gìn vệ sinh và tuân thủ các quy định của nhà trường.',1,'2026-10-30 00:56:52.880790','2026-10-30 00:56:52.880790'),
(43,8,2,'Discover KLF','KLF is a self-study area belonging to the Learning Resource Center of Saigon University, serving the study and research needs of students. This is a place where students can study independently, read materials, and engage in group study within a quiet space. When using this area, please maintain order, minimize noise, keep it clean, and comply with the university\'s regulations.',1,'2026-10-30 00:56:59.926436','2026-11-16 12:06:43.756475'),
(44,8,3,'探索 KLF','KLF 是西贡大学学习资源中心旗下的自习区，旨在满足学生们的学习和研究需求。这是一个学生可以在安静的空间里自习、阅读资料和进行小组学习的场所。使用该区域时，请保持安静、控制音量、讲究卫生并遵守学校的相关规定。',1,'2026-10-30 00:57:01.243759','2026-11-16 12:06:55.487678'),
(45,8,4,'KLF 탐방','KLF는 사이공대학교 학습리소스센터 산하의 자습 구역으로, 학생들의 학습 및 연구 수요를 지원합니다. 이곳은 학생들이 조용한 공간에서 자습하고, 자료를 읽고, 조별 스터디를 할 수 있는 장소입니다. 이 구역을 이용하실 때에는 정숙을 유지하고, 소음을 줄이며, 청결을 지키고 학교 규정을 준수해 주시기 바랍니다.',1,'2026-10-30 00:57:11.416568','2026-11-16 12:07:07.693009'),
(46,7,1,'Khám phá căn tin','Đây là khu căn tin trường Đại học Sài Gòn. Ở đây nổi tiếng món bún bò ngon nhất',1,'2026-11-16 09:59:30.855804','2026-11-16 09:59:30.855804'),
(47,7,2,'Exploring the cafeteria','This is the cafeteria area of Saigon University. It is famous for having the most delicious beef noodle soup.',1,'2026-11-16 10:00:10.801952','2026-11-16 10:00:10.801952'),
(48,7,3,'探索食堂','这里是西贡大学的食堂。这里的牛肉米粉以最美味而闻名。',1,'2026-11-16 10:00:19.150590','2026-11-16 10:00:19.150590'),
(49,7,4,'구내식당 탐방','이곳은 사이공 대학교 구내식당입니다. 이곳은 가장 맛있는 분보(베트남 쌀국수)로 유명합니다.',1,'2026-11-16 10:00:27.854795','2026-11-16 10:00:27.854795');
/*!40000 ALTER TABLE `narration_contents` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
INSERT INTO `password_reset_tokens` VALUES
(10,1,X'4a6d9c555145fe707cada1cc8b204ab2db98ac4100b9c1779bbf62cb9aedb231',X'5f79d2d4402409ed88033b44fcdbf475e96f6d4156768b6aab2f8878cbc5ca84',0,'2026-11-16 18:58:08.230532','2026-11-16 18:57:50.148324','2026-11-16 19:02:50.148324','2026-11-16 18:58:35.380470');
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
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `poi_images`
--

LOCK TABLES `poi_images` WRITE;
/*!40000 ALTER TABLE `poi_images` DISABLE KEYS */;
INSERT INTO `poi_images` VALUES
(1,9,'/poi-image-files/9/9e44458521c948aa92d921fb05dfb6b2.jpg',NULL,1,'2026-11-04 11:56:55.208415'),
(3,1,'/poi-image-files/1/4ea1d0f76c6c4594a046a3dc1b3361a3.png',NULL,1,'2026-11-06 08:28:15.884515'),
(6,6,'/poi-image-files/6/a2fa7f4ca72e4aa2a6490874f62c2ffb.jpg',NULL,1,'2026-11-06 08:54:05.308635'),
(7,5,'/poi-image-files/5/1d7c8280728543cc87c295856680fd65.jpg',NULL,1,'2026-11-14 01:05:07.199175'),
(8,13,'/poi-image-files/13/c8c1c5ea8fea4fa0b1cddbc59481ee54.png',NULL,1,'2026-11-14 01:06:45.483001'),
(9,12,'/poi-image-files/12/f88beb9a47564fa6a995be5b5a633896.jpg',NULL,1,'2026-11-14 01:23:14.482204'),
(10,4,'/poi-image-files/4/68fc16e9b896449c91e81c6dbca2be0a.png',NULL,1,'2026-11-14 01:34:18.525587'),
(11,15,'/poi-image-files/15/d9ae378af9f2487a84301342db0ad623.png',NULL,1,'2026-11-14 02:11:15.539456'),
(12,3,'/poi-image-files/3/dcbaaff20d4e40a1a4ab4f5d49ebff57.png',NULL,1,'2026-11-14 03:20:33.418557'),
(13,7,'/poi-image-files/7/5ee8ecdc5bdd40c68bbe733804666dca.png',NULL,1,'2026-11-14 04:00:56.351910'),
(14,11,'/poi-image-files/11/54c575653c0544f7b212320423e81a77.png',NULL,1,'2026-11-14 04:06:10.459432'),
(15,10,'/poi-image-files/10/de3f48fd74d549359b7ac886f4e081bb.png',NULL,1,'2026-11-14 04:15:11.258557'),
(16,16,'/poi-image-files/16/0ed77c6efef44298bac0de7346dfe408.png',NULL,1,'2026-11-14 08:16:39.377457'),
(17,14,'/poi-image-files/14/1cc4a089b6e142048ee661658d6c8e27.png',NULL,1,'2026-11-14 08:20:06.123308'),
(18,8,'/poi-image-files/8/f7cd226437f54aa6bfa2b356b7fd0ca5.png',NULL,1,'2026-11-14 15:11:33.590307'),
(19,2,'/poi-image-files/2/65a5963d91d249b9a5306da0e3717d29.png',NULL,1,'2026-11-14 15:12:05.287515');
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
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `points_of_interest`
--

LOCK TABLES `points_of_interest` WRITE;
/*!40000 ALTER TABLE `points_of_interest` DISABLE KEYS */;
INSERT INTO `points_of_interest` VALUES
(1,1,'SGU_SAN_BONG_CHUYEN','Sân bóng chuyền','Chào mừng bạn đến với sân bóng đá trong khuôn viên Trường Đại học Sài Gòn. Tại điểm dừng này, hãy dành một chút thời gian quan sát sân và không gian xung quanh. Nếu đang có trận đấu hoặc buổi tập, bạn hãy đứng ngoài đường biên, giữ khoảng cách an toàn và tránh đi ngang qua sân. Khi tiếp tục tham quan, hãy kiểm tra bản đồ để chọn địa điểm tiếp theo. Bạn có thể chuyển ngôn ngữ để đọc và nghe bài giới thiệu theo lựa chọn của mình.',10.7597778,106.6823611,40.00,1,'2026-10-25 00:34:44.058922','2026-11-16 09:51:35.796532'),
(2,1,'SGU_SAN_BONG_DA','Sân bóng đá','Sân bóng đá trong khuôn viên Trường Đại học Sài Gòn, một điểm dừng trong hành trình khám phá trường.',10.7596667,106.6814444,40.00,1,'2026-10-25 01:37:28.459822','2026-11-16 09:51:35.796532'),
(3,1,'SGU_TOA_B','Tòa B','Khu B trường Đại học Sài Gòn',10.7594618,106.6822493,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(4,1,'SGU_TOA_A','Tòa A','Khu A trường Đại học Sài Gòn',10.7590944,106.6824268,40.00,1,'2026-10-27 14:09:53.450025','2026-11-17 02:38:16.633041'),
(5,1,'SGU_TOA_E','Tòa E','Khu E trường Đại học Sài Gòn',10.7590667,106.6818546,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(6,1,'SGU_TOA_C','Tòa C','Khu C trường Đại học Sài Gòn',10.7599846,106.6821742,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(7,1,'SGU_CAN_TIN','Căn tin','Căn tin trường Đại học Sài Gòn',10.7595822,106.6819974,40.00,1,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(8,1,'SGU_KLF','KLF','Khu KLF trường Đại học Sài Gòn',10.7598791,106.6825604,40.00,1,'2026-10-27 14:09:53.450025','2026-11-16 09:58:43.226704'),
(9,1,'SGU_CONG_CHINH','Cổng chính','Cổng chính Đại học Sài Gòn',10.7603558,106.6819845,40.00,1,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(10,1,'SGU_KHU_HIEU_BO','Khu hiệu bộ','Khu hiệu bộ trường Đại học Sài Gòn',10.7604723,106.6822566,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(11,1,'SGU_KHU_D','Khu D','Khu D trường Đại học Sài Gòn',10.7602824,106.6817520,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(12,1,'SGU_SAN_BONG_RO','Sân bóng rổ','Sân bóng rổ trường Đại học Sài Gòn',10.7595443,106.6817209,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(13,1,'SGU_CONG_NGUYEN_TRAI','Cổng Nguyễn Trãi','Cổng phụ Nguyễn Trãi trường Đại học Sài Gòn',10.7587678,106.6818855,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(14,1,'SGU_TRAM_BUS_AN_DUONG_VUONG','Trạm Bus An Dương Vương','Trạm Bus Cổng An Dương Vương trường Đại học Sài Gòn',10.7606298,106.6822570,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(15,1,'SGU_KHU_HOI_TRUONG_A','Khu hội trường A','Khu Hội trường A trường Đại học Sài Gòn',10.7596352,106.6830235,40.00,1,'2026-10-27 14:09:53.450025','2026-11-17 02:38:01.474370'),
(16,1,'SGU_BAI_GUI_XE','Bãi gửi xe','Bãi gửi xe trường Đại học Sài Gòn',10.7592550,106.6817160,40.00,0,'2026-10-27 14:09:53.450025','2026-11-16 09:51:35.796532'),
(17,1,'SGU_030','Hội trường E','Khám phá hội trường E',10.7590666,106.6818545,0.06,0,'2026-10-30 01:34:56.715093','2026-11-14 08:20:14.626336');
/*!40000 ALTER TABLE `points_of_interest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tour_route_pois`
--

DROP TABLE IF EXISTS `tour_route_pois`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tour_route_pois` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `route_id` bigint unsigned NOT NULL,
  `poi_id` bigint unsigned DEFAULT NULL,
  `sort_order` smallint unsigned NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `instructions` text,
  `location_label` varchar(200) DEFAULT NULL,
  `duration_minutes` smallint unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_route_position` (`route_id`,`sort_order`),
  KEY `fk_routepoi_poi` (`poi_id`),
  CONSTRAINT `fk_routepoi_poi` FOREIGN KEY (`poi_id`) REFERENCES `points_of_interest` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_routepoi_route` FOREIGN KEY (`route_id`) REFERENCES `tour_routes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_routepoi_order` CHECK ((`sort_order` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tour_route_pois`
--

LOCK TABLES `tour_route_pois` WRITE;
/*!40000 ALTER TABLE `tour_route_pois` DISABLE KEYS */;
INSERT INTO `tour_route_pois` VALUES
(59,4,NULL,1,'Chuẩn bị hồ sơ','Kiểm tra danh mục giấy tờ theo hướng dẫn chính thức.','Trước khi đến trường',0),
(60,4,NULL,2,'Đối chiếu giấy tờ','Kiểm tra và sắp xếp giấy tờ trước khi nộp.','Điểm tiếp nhận do SGU thông báo',10),
(61,4,NULL,3,'Nộp và xác nhận','Nộp theo hướng dẫn và nhận giấy xác nhận nếu được cấp.','Quầy tiếp nhận do SGU thông báo',20),
(62,3,NULL,1,'Xem lịch sinh hoạt','Đọc lịch và địa điểm từ thông báo chính thức.','Tra cứu online',0),
(63,3,NULL,2,'Đến điểm tập trung','Đến địa điểm được thông báo và điểm danh.','Theo thông báo',10),
(64,3,NULL,3,'Tham gia sinh hoạt','Nghe các nội dung phổ biến đầu khóa.','Phòng hoặc hội trường được thông báo',45),
(65,3,NULL,4,'Nhận thông tin tiếp theo','Lưu lại lịch học và kênh liên hệ.','Theo hướng dẫn',5),
(71,5,2,1,'Tham quan sân bóng đá','Di chuyển theo lối đi chung, không vào sân khi có hoạt động.','Sân bóng đá',8),
(72,5,1,2,'Tham quan sân bóng chuyền','Giữ khoảng cách an toàn và không làm gián đoạn hoạt động.','Sân bóng chuyền',7),
(93,2,NULL,1,'Tra cứu lịch khám','Kiểm tra lịch, giấy tờ và phiếu khám theo thông báo.','Trước ngày khám',0),
(94,2,NULL,2,'Đến điểm tiếp nhận','Đến đúng địa điểm và giờ hẹn.','Chờ SGU xác nhận địa điểm',5),
(95,2,NULL,3,'Nhận hướng dẫn khám','Xác nhận thông tin và nhận phiếu theo yêu cầu.','Bàn đăng ký',10),
(96,2,NULL,4,'Khám theo chỉ dẫn','Thực hiện từng hạng mục theo nhân viên y tế.','Các bàn khám',40),
(97,2,NULL,5,'Hoàn tất khám','Kiểm tra và nộp phiếu theo hướng dẫn.','Điểm hoàn tất',10),
(98,1,NULL,1,'Chuẩn bị và kiểm tra hồ sơ','Đối chiếu danh mục giấy tờ trong thông báo tuyển sinh chính thức.','Trước khi đến SGU',0),
(99,1,NULL,2,'Đến điểm hướng dẫn nhập học','Tìm bảng chỉ dẫn hoặc nhân viên hỗ trợ để biết đúng khu vực tiếp nhận.','Chờ SGU xác nhận địa điểm',5),
(100,1,NULL,3,'Nộp hồ sơ trực tiếp','Nộp hồ sơ theo hướng dẫn bộ phận tiếp nhận.','Quầy tiếp nhận (chưa xác nhận)',15),
(101,1,NULL,4,'Đối chiếu thông tin','Kiểm tra dữ liệu cá nhân và hoàn thiện yêu cầu bổ sung nếu có.','Khu vực kiểm tra hồ sơ',10),
(102,1,NULL,5,'Xác nhận hoàn tất','Nhận và lưu lại hướng dẫn về các thủ tục tiếp theo.','Theo hướng dẫn của trường',5);
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
  `audience` varchar(50) DEFAULT NULL,
  `is_template` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_route_code` (`route_code`),
  CONSTRAINT `ck_route_code` CHECK ((char_length(trim(`route_code`)) > 0)),
  CONSTRAINT `ck_route_duration` CHECK ((`estimated_duration_minutes` > 0)),
  CONSTRAINT `ck_route_name` CHECK ((char_length(trim(`route_name`)) > 0)),
  CONSTRAINT `ck_route_visible` CHECK ((`is_visible` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tour_routes`
--

LOCK TABLES `tour_routes` WRITE;
/*!40000 ALTER TABLE `tour_routes` DISABLE KEYS */;
INSERT INTO `tour_routes` VALUES
(1,'NHAP_HOC_TRUC_TIEP','Thủ tục nhập học trực tiếp','Lộ trình mẫu tham khảo, kiểm tra thông báo chính thức của SGU trước khi đi.',35,0,'2026-11-16 19:13:32.079322','2026-11-20 09:26:48.254902','Tân sinh viên',0),
(2,'KHAM_SUC_KHOE_DAU_KHOA','Khám sức khỏe nhập học','Thứ tự tham khảo; thời gian và phòng khám do nhà trường công bố.',65,1,'2026-11-16 19:13:32.079322','2026-11-20 18:17:48.135285','Tân sinh viên',0),
(3,'SINH_HOAT_DAU_KHOA','Sinh hoạt đầu khóa','Hướng dẫn tổng quan, xác nhận lịch của nhà trường.',60,1,'2026-11-16 19:13:32.079322','2026-11-20 07:59:09.705664','Tân sinh viên',0),
(4,'NOP_HO_SO','Nộp hồ sơ','Hướng dẫn chuẩn bị và nộp hồ sơ, theo thông báo chính thức.',30,0,'2026-11-16 19:13:32.079322','2026-11-20 09:17:27.721645','Tân sinh viên',0),
(5,'THAM_QUAN_KHUON_VIEN','Tham quan khuôn viên','Lộ trình tham quan mẫu nối các POI đã có tọa độ.',15,1,'2026-11-16 19:13:32.079322','2026-11-20 07:15:59.893830','Tân sinh viên',0);
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
) ENGINE=InnoDB AUTO_INCREMENT=367 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visitor_sessions`
--

LOCK TABLES `visitor_sessions` WRITE;
/*!40000 ALTER TABLE `visitor_sessions` DISABLE KEYS */;
INSERT INTO `visitor_sessions` VALUES
(1,X'7e3a493329a55a80ed46adb1bd05f47ed8674d80c924687bbf862f448ec1a0c1','2026-10-24 15:15:37.656462','2026-10-24 15:45:48.505956','2026-10-24 15:15:49.227415','2026-10-24 15:15:48.505956',0),
(2,X'c232df13454fe1cf00bcb4625bb42329c42ae0a14288197700d9a53be0e86a07','2026-10-24 15:15:49.281926','2026-10-24 15:46:00.225591','2026-10-24 15:16:05.600472','2026-10-24 15:16:00.225591',0),
(3,X'289e3263678735c5631b0d508fcc196bcc69f375bfa3ef68a3fb76e230f3e6fb','2026-10-24 15:26:31.359759','2026-10-24 16:01:07.557590','2026-10-24 15:31:09.524993','2026-10-24 15:31:07.557590',0),
(4,X'24e1c9520ee4e4027214c53c975c2235a939a1c8b57ceb85d91f58d50d5f8290','2026-10-24 15:31:09.544841','2026-10-24 16:01:13.760119','2026-10-24 15:31:15.644922','2026-10-24 15:31:13.760119',0),
(5,X'd4f315d7034d84fb47a9179144ff0091ff2d815f81f96ba2acb5f97d17943774','2026-10-24 15:31:15.664889','2026-10-24 16:20:33.056615','2026-10-24 15:50:33.816275','2026-10-24 15:50:33.056615',0),
(6,X'c0694047b637505824e31acebef1dc29f02539bbd23e0f37ce1351607ba6a817','2026-10-24 15:50:33.848082','2026-10-24 16:20:33.859347','2026-10-24 15:50:34.564056','2026-10-24 15:50:33.859347',0),
(7,X'ec2a04db34d42b3d5d9381692682d1e740f36dafed149b615f051465db64c74a','2026-10-24 15:50:34.581924','2026-10-24 16:20:34.592743','2026-10-24 15:50:34.715588','2026-10-24 15:50:34.592743',0),
(8,X'ab7aba748a8b0fa075d31b717de3a5976e04af8da0b0f041c0bc8c0f6acfb9e4','2026-10-24 15:50:34.738526','2026-10-24 16:20:34.748072','2026-10-24 15:50:34.875340','2026-10-24 15:50:34.748072',0),
(9,X'0f142f713c1cac2a7f46e6d7d3dadb18a596a0aa17d54eda58a5ccbf850fe183','2026-10-24 15:50:34.895480','2026-10-24 16:20:34.904354','2026-10-24 15:50:35.173794','2026-10-24 15:50:34.904354',0),
(10,X'ccee7faa75152e87919a7bbd16bad2c7beebc9064149af0a4b8b39f2a4fe4e2f','2026-10-24 15:50:35.198270','2026-10-24 16:26:45.196725','2026-10-24 15:56:48.595630','2026-10-24 15:56:45.196725',0),
(11,X'baa1ca9e70df53d662cedb583b3d62168aac54d1282d1d573515865a05a30e24','2026-10-24 17:04:58.294774','2026-10-24 17:40:17.446528','2026-10-24 17:10:19.048235','2026-10-24 17:10:17.446528',0),
(12,X'73ad4c3afe1aa24830496e4515b9826afb83e4e6796296defcf4ab75888c7fd2','2026-10-24 17:10:19.074793','2026-10-24 17:43:59.066949','2026-10-24 17:14:06.219248','2026-10-24 17:13:59.066949',0),
(13,X'69d8bac5da7930a446603764781af15940ec7379ea40ae9781baf940ec3191f5','2026-10-25 14:34:16.255543','2026-10-25 15:04:16.255543',NULL,'2026-10-25 14:34:16.255543',0),
(14,X'bb7f61fd09dfc049d164f6d3e0b18cb77ac66c1a466d6cd4774cc303b08e7873','2026-10-25 14:52:17.353000','2026-10-25 15:27:23.050646',NULL,'2026-10-25 14:57:23.050646',0),
(15,X'b23a9edc62791fdc82462d3a78cb47729eb12b0ec2186209fc6a99650b3b704c','2026-10-25 15:23:45.510971','2026-10-25 16:04:28.511329','2026-10-25 15:34:30.691284','2026-10-25 15:34:28.511329',0),
(16,X'2b8883a89a19272c14d456ce45aca30494aa8de8f5c5ba2816390328409113d6','2026-10-25 15:23:57.755981','2026-10-25 15:53:57.901256',NULL,'2026-10-25 15:23:57.901256',0),
(17,X'7b37fd8c48f08340efe220b77520edf5a1a00bca77dabcf6cd667a8caf3d6bed','2026-10-25 15:24:00.887802','2026-10-25 15:56:11.417649','2026-10-25 15:26:13.090354','2026-10-25 15:26:11.417649',0),
(18,X'9cc1f46a80620477dc6ebe262f7498850d02afa5ce6815296a59f0c1c5b60df5','2026-10-25 15:24:39.148872','2026-10-25 15:56:59.891089','2026-10-25 15:27:01.389159','2026-10-25 15:26:59.891089',0),
(19,X'0246a33e32b5d7d8526152551146c885f1682d19447023c6f8d13a346eacbf67','2026-10-25 15:26:13.466806','2026-10-25 16:06:17.090900',NULL,'2026-10-25 15:36:17.090900',1),
(20,X'c157f5fff0a30b47a80e4714f6fbb339416afa23ee8d01eea0b23d464bdb7100','2026-10-25 15:27:01.892875','2026-10-25 17:55:17.881225',NULL,'2026-10-25 17:25:17.881225',0),
(21,X'd70c9996ac404cd32fe63aa860a42159f126822ba1d6e900f1536b4e201107aa','2026-10-25 15:34:34.162835','2026-10-25 16:04:34.162835',NULL,'2026-10-25 15:34:34.162835',0),
(22,X'861b1085d0d57c773e6a6bfe4753d37038dbb61da59a9fae1330258c3b5aaf53','2026-10-25 15:34:35.014573','2026-10-25 16:04:35.134086','2026-10-25 15:34:36.657166','2026-10-25 15:34:35.134086',0),
(23,X'edd377d514a4f7997b1b6c9b554b33038b949d9390b316a775ec781e3e652e7b','2026-10-25 15:34:36.913282','2026-10-25 16:04:42.331670','2026-10-25 15:34:43.811065','2026-10-25 15:34:42.331670',0),
(24,X'4f2392f9afa2c113765ace67175d7b744f24ecb8e0b415361110eff5747e7e7b','2026-10-25 15:34:44.088277','2026-10-25 17:56:24.128587','2026-10-25 17:26:33.050862','2026-10-25 17:26:24.128587',0),
(25,X'5871764052c3e5df2a27405295d8c7d00747025375e623ac6fd02905bccda3a3','2026-10-26 08:04:08.662921','2026-10-26 09:09:56.708520','2026-10-26 08:39:58.974965','2026-10-26 08:39:56.708520',0),
(26,X'5de268b830cb3e16a32aef53b56a5e21aa7f61cd976b3b01bffbb88e196972d4','2026-10-26 08:39:59.052469','2026-10-26 09:18:58.997942','2026-10-26 08:48:59.170666','2026-10-26 08:48:58.997942',0),
(27,X'6003c1e15e242dcb905ab175bfa795203cc434f832c7b9042584643f806ec552','2026-10-26 09:49:16.847965','2026-10-26 10:28:16.783567',NULL,'2026-10-26 09:58:16.783567',0),
(28,X'6888ae5406d25d72c2504eecd36686b3cbd354c2a556e5a9861ceffa428717e4','2026-10-26 09:50:04.575464','2026-10-26 10:20:04.980880',NULL,'2026-10-26 09:50:04.980880',0),
(29,X'52fbd415e10dc6c27ffd7fb2a621b322f7b393bf3b72e6030df3d8526acfefa0','2026-10-26 09:58:12.530411','2026-10-26 10:30:21.920639','2026-10-26 10:00:22.103939','2026-10-26 10:00:21.920639',0),
(30,X'3a8976996240378cad90989f0de6e681d4104c3af546966809b35980e5351cf7','2026-10-26 09:58:17.761221','2026-10-26 10:28:18.934970',NULL,'2026-10-26 09:58:18.934970',0),
(31,X'661db85b945867787370f065e9c3ba0d27f1ba96330f5ec367779a24a7141ccd','2026-10-26 10:00:12.555652','2026-10-26 10:30:22.566811','2026-10-26 10:01:10.130959','2026-10-26 10:00:22.566811',0),
(32,X'51c67a2f27dd5e67409f85aa13d1cfcfcdc252266cb18e99832141209d30d013','2026-10-26 10:00:24.214855','2026-10-26 10:30:33.033383','2026-10-26 10:02:29.145849','2026-10-26 10:00:33.033383',0),
(33,X'2fb6ea6e205d3c344239bbe8c29831b4157c05ea8dc9d21dbb43e39cf09b96fc','2026-10-26 10:00:24.347355','2026-10-26 10:30:24.347355',NULL,'2026-10-26 10:00:24.347355',0),
(34,X'aafeec5a6fd0b214062b82cc7c36f0e1c045d8204f611a510a77edc75dd77de3','2026-10-26 10:00:25.552473','2026-10-26 10:30:28.682517','2026-10-26 10:00:31.613229','2026-10-26 10:00:28.682517',0),
(35,X'e9824dcc14508b63d55dcf6786d10a2b5a337ef1a8c7800af24eaf7a9b2ae80d','2026-10-26 10:00:32.076969','2026-10-26 10:30:32.749359',NULL,'2026-10-26 10:00:32.749359',0),
(36,X'fe2b1bb1197be7a80ee96ce624e59d53e07cda826d75e53dafba6c05350143e0','2026-10-26 10:00:32.284021','2026-10-26 10:59:16.810111','2026-10-26 10:29:46.132696','2026-10-26 10:29:16.810111',0),
(37,X'f50de6af1ca5be72dad715e17a78886101e18019e7fad6675c7a2d4c04006d19','2026-10-26 10:01:49.051969','2026-10-26 10:56:53.084725','2026-10-26 10:26:56.054682','2026-10-26 10:26:53.084725',0),
(38,X'80feb230c77070ae42040b211100a2f01b17526cd653834f5eaba6f6f102d911','2026-10-26 10:02:00.547326','2026-10-26 10:43:39.439793','2026-10-26 10:13:39.980054','2026-10-26 10:13:39.439793',0),
(39,X'5d84d2bf1d41de5aaeb7e2238f8ca7cef3ebe8496daca704ed16a14ebe5b5475','2026-10-26 10:02:45.806523','2026-10-26 10:32:45.968717',NULL,'2026-10-26 10:02:45.968717',0),
(40,X'2ac6a261ca3c3bc89a77298bb6b9e28fc7a7a28c3b88e3c9675b9fcad8c7a74e','2026-10-26 10:02:47.056226','2026-10-26 10:32:47.614935',NULL,'2026-10-26 10:02:47.614935',0),
(41,X'a7623a53b1fd52aac309c6450f33e66c58dcf493d68fc990c3f8a5743a33b6ac','2026-10-26 10:03:12.547306','2026-10-26 10:33:38.261615',NULL,'2026-10-26 10:03:38.261615',0),
(42,X'06f937d33ae00b47f0b493543893d48022f1e68bc029b5925ceebc22d7da67f0','2026-10-26 10:07:12.467758','2026-10-26 10:37:21.905886',NULL,'2026-10-26 10:07:21.905886',0),
(43,X'c6c6438c0cff1bf7fcb9f95f73d0f0f0eeed49d97d588a91b6029cfe802f8bae','2026-10-26 10:13:44.131769','2026-10-26 10:50:59.515022','2026-10-26 10:21:00.051005','2026-10-26 10:20:59.515022',0),
(44,X'4e840aa69bf5aa860435d42f0b72db3ed841410a416512c49efe112951f6bacf','2026-10-26 10:13:49.978978','2026-10-26 10:43:52.702742','2026-10-26 10:13:59.710665','2026-10-26 10:13:52.702742',0),
(45,X'8b995b8a8a0b7b2fc4f583526a7ed50b5e40de384362fa29ea9e0ae8f7d7cff4','2026-10-26 10:16:12.994143','2026-10-26 11:00:52.975677','2026-10-26 10:30:53.142537','2026-10-26 10:30:52.975677',0),
(46,X'2a872d33656d37cbb947765a8324234a7932d2c704aa9a41a9f50746e1a5426e','2026-10-26 10:16:22.498525','2026-10-26 10:46:23.122750',NULL,'2026-10-26 10:16:23.122750',0),
(47,X'3627ff00068ab6742a2a67b4242275b6d99041bfe10c6093a348ad810f3d3dce','2026-10-26 10:22:13.163717','2026-10-26 11:02:51.066392',NULL,'2026-10-26 10:32:51.066392',0),
(48,X'0946fb0fa6f1e8086566e5e10cf9a4c896b47e76dbde4983e4822bd5661974a4','2026-10-26 10:26:56.677635','2026-10-26 10:57:06.323278',NULL,'2026-10-26 10:27:06.323278',0),
(49,X'81f5dd773090b947b43b3ca4835514358f2763560e9cf4066ed6f652c4d19340','2026-10-26 10:26:57.520319','2026-10-26 11:00:53.665405','2026-10-26 10:30:54.177994','2026-10-26 10:30:53.665405',0),
(50,X'28971c5a6c61f65fa8c6c89218d91b9ecc2f0a2aefc1f036566d1c1667ddc286','2026-10-26 10:30:22.812645','2026-10-26 11:00:37.788083',NULL,'2026-10-26 10:30:37.788083',0),
(51,X'a9a35e024eed10c147df6e13049f6b2fccefe739eecfc8aadf42b43025909ee5','2026-10-26 10:30:52.591659','2026-10-26 11:01:09.737070','2026-10-26 10:31:17.641594','2026-10-26 10:31:09.737070',0),
(52,X'a477d842fa9936f4e6ef4386fd96e23a940acf616007754ec108f333b1d23807','2026-10-26 10:31:19.167128','2026-10-26 11:02:51.505931',NULL,'2026-10-26 10:32:51.505931',0),
(53,X'c99c327a4e1b3523fc58b39230f3da38395148c35d25e3d41cd603357b4e8247','2026-10-26 17:29:21.445610','2026-10-26 18:51:51.055729','2026-10-26 18:21:51.903315','2026-10-26 18:21:51.055729',0),
(54,X'e6b68bf283987613726824563a4305f5527e871efc581063d04fd56aeea2f0d9','2026-10-26 18:21:53.233941','2026-10-26 19:21:17.008357',NULL,'2026-10-26 18:51:17.008357',0),
(55,X'fb7a23edc4852442f89514ccebed64f964279e05b19717bf1f5a9918300f1b12','2026-10-26 18:32:26.078486','2026-10-26 19:04:08.930180','2026-10-26 18:34:11.149317','2026-10-26 18:34:08.930180',0),
(56,X'648a56de5e4926b85e281b669146c41973671c1ec7d9b1497a294ba15e7ebb98','2026-10-26 18:34:12.366794','2026-10-26 19:04:20.676814','2026-10-26 18:34:27.879573','2026-10-26 18:34:20.676814',0),
(57,X'6732fa12d02ad8b87d788c28512f9be44da208acbd7a27312484e379a078ff1a','2026-10-26 18:56:24.305184','2026-10-26 19:27:37.025209',NULL,'2026-10-26 18:57:37.025209',0),
(58,X'9b84ccc58bec2411980a042e32e5dd01767e883d0d054739e9af29cf515b4ef3','2026-10-26 18:56:35.238573','2026-10-26 19:26:35.765338',NULL,'2026-10-26 18:56:35.765338',0),
(59,X'cc16ac292f43a6f4876f1f277141c6f7b8632666703b5c263c6afccafc6fa240','2026-10-26 18:56:57.349420','2026-10-26 19:26:57.979114',NULL,'2026-10-26 18:56:57.979114',0),
(60,X'b95f6a3983031d85bffcbd3e41254b9249c1c135846403a3c06c954274e0cdeb','2026-10-26 18:57:08.250780','2026-10-26 19:27:08.853155',NULL,'2026-10-26 18:57:08.853155',0),
(61,X'b4339b55fadf0e03bbc26704adc6cb01e5c339139fb1c1722ab83d3fd987ce56','2026-10-26 19:01:12.077171','2026-10-26 19:32:25.491031','2026-10-26 19:02:30.680189','2026-10-26 19:02:25.491031',0),
(62,X'e07ac153b51dd7979b35b9589d8f5e2d4a0503aaa91900d1664e03b01b2b2506','2026-10-26 19:03:00.961830','2026-10-26 19:33:02.547510',NULL,'2026-10-26 19:03:02.547510',0),
(63,X'5c0724aea0c0a4c771e6933cf940a60b6392d464673c8fadbfc3e3c448b7eb57','2026-10-26 19:42:51.253542','2026-10-26 20:12:51.742176','2026-10-26 19:42:56.012641','2026-10-26 19:42:51.742176',0),
(64,X'a2c0200b2672e178dc808849065dbf88c01d1890da113be687251f86fd8e9366','2026-10-27 06:38:12.561637','2026-10-27 07:08:13.270987',NULL,'2026-10-27 06:38:13.270987',0),
(65,X'945af8422f0dd2912489f1f1bf29e867f4751535c5652d363a95c84fd955c24e','2026-10-27 06:38:13.954106','2026-10-27 07:08:35.485670','2026-10-27 06:38:44.716713','2026-10-27 06:38:35.485670',0),
(66,X'd3d1ab30ae3e2e5a68baf9385b898e3913f073d5ca2c71fa1dfecc0dd1af2be7','2026-10-27 06:38:14.155375','2026-10-27 07:08:44.812201','2026-10-27 06:38:45.326710','2026-10-27 06:38:44.812201',0),
(67,X'3741aa81914ab550b740df7b8d197799f37d2b6dff12a5536415ccaa80d8d0d1','2026-10-27 06:39:46.993176','2026-10-27 07:10:03.169088',NULL,'2026-10-27 06:40:03.169088',0),
(68,X'99020f992301b1c97797e64a2edb107402d4dad1cd157c8ab7eff632198990dc','2026-10-27 06:42:20.246253','2026-10-27 07:12:20.613900',NULL,'2026-10-27 06:42:20.613900',0),
(69,X'b449ce885503129f583dae8bb26fc0f9b020dd1cb33768887bff42366fdb7766','2026-10-27 06:42:28.931354','2026-10-27 07:12:55.847365',NULL,'2026-10-27 06:42:55.847365',0),
(70,X'65b6e1c519ced4289d1f6c242f368f5653b52935f42803a50655b46e6cdb628b','2026-10-27 07:25:17.828682','2026-10-27 07:55:18.202929','2026-10-27 07:25:23.508677','2026-10-27 07:25:18.202929',0),
(71,X'13e84f865d4105c87a86c482a8ac362f2c72967a58a2cb4e249d4a6bed9878f4','2026-10-27 09:17:06.005759','2026-10-27 09:47:06.250205','2026-10-27 09:17:10.457660','2026-10-27 09:17:06.250205',0),
(72,X'efb86421179a1942e557d581d7006f844c2b4ee1c8cf4e6cc542d35efbae0b78','2026-10-27 13:51:44.527166','2026-10-27 14:23:34.568069','2026-10-27 13:53:35.741834','2026-10-27 13:53:34.568069',0),
(73,X'67f5cac2ef3d278cca6615644b7a76e00f18b682c97fffbb4c77836ca891f71b','2026-10-27 13:53:37.317880','2026-10-27 14:40:03.851408','2026-10-27 14:10:04.828869','2026-10-27 14:10:03.851408',0),
(74,X'e0b8bf5a9c5ccd8bfe0c8c546c4c2575ee284d84b78c62aa4aa38d929fa040e1','2026-10-27 14:10:06.190625','2026-10-27 14:43:06.720361','2026-10-27 14:13:11.264853','2026-10-27 14:13:06.720361',0),
(75,X'b3f468de692bd6f3b34554a49a6c569595873970e2b8271f379c150e184fe692','2026-10-27 14:12:53.054711','2026-10-27 14:43:43.061848','2026-10-27 14:13:47.135898','2026-10-27 14:13:43.061848',0),
(76,X'a103135d0f6985a6c221ef9f4c87c462a5d7fb1cab8dc65217ca5b0433ec50ba','2026-10-27 14:13:03.338899','2026-10-27 14:43:04.033907',NULL,'2026-10-27 14:13:04.033907',0),
(77,X'87cb6e302f2febed8ccfa44c229ca7f59ee469f76479d4552eb89d232de67e6a','2026-10-27 14:13:11.287959','2026-10-27 14:43:11.902756',NULL,'2026-10-27 14:13:11.902756',0),
(78,X'3efa011799d46afb88855a5188d32baf1f1f0dfb52af05b3013ff6593ad5978a','2026-10-27 14:13:12.769686','2026-10-27 14:45:50.883734','2026-10-27 14:15:52.399752','2026-10-27 14:15:50.883734',0),
(79,X'0a9494c3a87b05837b363de30616955e391f2b1d856ede5ea03d4df6536e4157','2026-10-27 14:22:02.859761','2026-10-27 14:52:03.324379',NULL,'2026-10-27 14:22:03.324379',0),
(80,X'159a1669a2b8daa2d925ae076afe39d1f54ae5fdc877fd5c5a7a8ee04b968806','2026-10-27 15:05:48.853855','2026-10-27 15:35:49.427183',NULL,'2026-10-27 15:05:49.427183',0),
(81,X'dd56b4cf18c9797e8fec6a9af34baf5c4b72acb5e96119cbd292acd4754f7c95','2026-10-27 15:06:14.695174','2026-10-27 15:37:18.286767',NULL,'2026-10-27 15:07:18.286767',0),
(82,X'c84137bdd113da6adfa8298a6d524f708615a23ca10d8bb5b63ecf2ae5d6bcd3','2026-10-28 07:18:08.511543','2026-10-28 08:02:35.016245','2026-10-28 07:32:36.305975','2026-10-28 07:32:35.016245',0),
(83,X'1313a9d476d4717a99971f182ca495fd4335d2a6a735a713927298ad92e0c5b2','2026-10-28 07:18:23.029836','2026-10-28 07:48:30.986431','2026-10-28 07:18:36.378682','2026-10-28 07:18:30.986431',0),
(84,X'c05c3b6669d1aac1d4f5c231c3da23dddaadb5533ee01e5ba184c0eed709078c','2026-10-28 07:18:27.202171','2026-10-28 07:48:27.749396',NULL,'2026-10-28 07:18:27.749396',0),
(85,X'5600d0b212c3843c6ecee319030d9d7e6e4c78e422e416bb6169221a17b88372','2026-10-28 07:18:42.189150','2026-10-28 07:48:43.447223','2026-10-28 07:18:47.675326','2026-10-28 07:18:43.447223',0),
(86,X'5a12346bf85014976135390ad3d0d628435e318017778334857854b0077a0dd6','2026-10-28 07:19:48.184335','2026-10-28 07:49:52.886780','2026-10-28 07:19:58.297635','2026-10-28 07:19:52.886780',0),
(87,X'fbb4c99bd6a9533605366367d83728bd82c04975161151683366968ee2119dea','2026-10-28 07:19:59.757709','2026-10-28 07:50:19.409967','2026-10-28 07:20:27.678066','2026-10-28 07:20:19.409967',0),
(88,X'51d518c80291c7cec62173f51689970d36390ec2d6f92e27a16b5b7a059660e8','2026-10-28 07:36:19.356651','2026-10-28 08:06:19.965926',NULL,'2026-10-28 07:36:19.965926',0),
(89,X'dd95c0550d2be5ac47b116aaa114f7f5b60c6940ace61baa521b08f90bd7cc99','2026-10-28 07:42:37.088378','2026-10-28 08:12:37.674360',NULL,'2026-10-28 07:42:37.674360',0),
(90,X'acc33bb4a4e28c51b04a82acd3751e080ee08db524559d8bc8e6934bf2ca306f','2026-10-28 07:58:52.915980','2026-10-28 08:33:22.260247',NULL,'2026-10-28 08:03:22.260247',0),
(91,X'ae100be9ff93043a4e6a15bb090d19c755c20c0cd6a09bcc6ecdff03902ab7ef','2026-10-28 07:59:11.538356','2026-10-28 08:29:22.532449','2026-10-28 07:59:28.457834','2026-10-28 07:59:22.532449',0),
(92,X'7a0d0676f6e85f645c2aaad99ae40b3d0e7e3843c54df0966af7058b0843f71d','2026-10-28 08:02:35.239663','2026-10-28 08:35:04.779015','2026-10-28 08:05:11.330740','2026-10-28 08:05:04.779015',0),
(93,X'411402c6b918b2d0de9f8a2bed4984590e51b639ace8ee52a5e0f998340483b3','2026-10-28 08:05:13.208378','2026-10-28 08:59:25.149063',NULL,'2026-10-28 08:29:25.149063',0),
(94,X'78722657e53a7c50cca010b450efc6fffd92024a3d3f4d2972bb56689edc7ff9','2026-10-28 14:36:12.454331','2026-10-28 15:06:51.928376',NULL,'2026-10-28 14:36:51.928376',0),
(95,X'a4e0513706bc6200fda9be6edae14ae36d56a5be07751ee925c6bffa891548ab','2026-10-28 14:36:59.548955','2026-10-28 15:07:00.160291',NULL,'2026-10-28 14:37:00.160291',0),
(96,X'4ca115de5b9b85951a0b5f95bb366ebac4468ba947dfdaf62baa7262f56b9642','2026-10-28 15:46:37.299023','2026-10-28 16:33:37.293732','2026-10-28 16:03:42.010861','2026-10-28 16:03:37.293732',0),
(97,X'7469a1bd287cbea109031d0beb59b866d2b542741c506de54733857ee23fca27','2026-10-28 16:03:43.077406','2026-10-28 16:33:43.358346','2026-10-28 16:03:48.414610','2026-10-28 16:03:43.358346',0),
(98,X'd2215bcd11490aaacd52082a040f1deabd8566ee154e013f17b3da79f7600cd4','2026-10-28 16:04:16.357688','2026-10-28 16:53:35.281128',NULL,'2026-10-28 16:23:35.281128',0),
(99,X'e1120ca2f6fd0d2b861150ebb69e04b08fd67d7338100d83955eb763b6bd8a9b','2026-10-28 23:32:49.727525','2026-10-29 00:02:50.253472',NULL,'2026-10-28 23:32:50.253472',0),
(100,X'f22f806bcdbfc16a6b339e23ae8f2607bacd4dd725de601eb4c937ca4fc4b798','2026-10-28 23:32:49.757327','2026-10-29 00:02:58.848900',NULL,'2026-10-28 23:32:58.848900',0),
(101,X'e5bda1ad71cbcbefe1295ee42ec3efb647ef846f391cda45ebbe6e5ce1da2520','2026-10-29 03:03:33.768338','2026-10-29 03:33:34.021665','2026-10-29 03:03:37.042327','2026-10-29 03:03:34.021665',0),
(102,X'6d8500581fab7e4767f40463b8328ad4c6d6e2e6b45495bdf205b5b1df6059dd','2026-10-29 08:24:20.402827','2026-10-29 08:54:46.714327','2026-10-29 08:24:54.414964','2026-10-29 08:24:46.714327',0),
(103,X'733f1d95a0edb7c6f209c5be7241f2cb6f17e720e4adacaaa0c400589398a8db','2026-10-29 09:28:02.958832','2026-10-29 11:06:20.977617','2026-10-29 10:36:22.959488','2026-10-29 10:36:20.977617',0),
(104,X'3b2ed580af8749101dcde8f4b25b312ad736337b6fbd109048093c441ceba835','2026-10-29 09:28:13.363336','2026-10-29 09:58:13.979899',NULL,'2026-10-29 09:28:13.979899',0),
(105,X'7aeb25b2139f718d2bda0e6da4c98e36cc59de033e9d453ecdf6d36c9e982aa9','2026-10-29 10:36:24.130813','2026-10-29 11:41:54.519293','2026-10-29 11:11:58.632567','2026-10-29 11:11:54.519293',0),
(106,X'a04e5e5e4c8a17a8f873801f3c24e2081a8391ed0b40a3f4efb21b9ab2c9df01','2026-10-29 11:11:59.731601','2026-10-29 11:42:17.263223','2026-10-29 11:12:21.344341','2026-10-29 11:12:17.263223',0),
(107,X'9f220b22ab2a0a531e45bdaec00fc8d5b627ef62afd31cf655d7244b5d578a89','2026-10-29 11:12:22.382337','2026-10-29 12:02:19.693766','2026-10-29 11:32:20.059080','2026-10-29 11:32:19.693766',0),
(108,X'9c8c08cedbb05aa50e80ec3297d96052c6c284289d3a365251ffc7c1e16f9410','2026-10-29 11:26:53.467315','2026-10-29 12:02:24.070942','2026-10-29 11:32:24.982467','2026-10-29 11:32:24.070942',0),
(109,X'374ccc8e0489ccdcedb93edcef69acf54867d7519d94ac089ba3aeaec60e421d','2026-10-29 11:32:26.218962','2026-10-29 12:07:31.091177','2026-10-29 11:37:33.626336','2026-10-29 11:37:31.091177',0),
(110,X'775222655fcf9897ca886b741dddf2e35a5f32a90108a29b97a5250745bd6578','2026-10-29 11:37:34.920506','2026-10-29 12:08:00.329534','2026-10-29 11:38:02.720013','2026-10-29 11:38:00.329534',0),
(111,X'c2622c28f6002bbbbbfea2b4377eee53e02eb4a95bb02fca07b679dc859c9dc7','2026-10-29 11:38:03.932410','2026-10-29 12:09:11.267333','2026-10-29 11:39:12.450125','2026-10-29 11:39:11.267333',0),
(112,X'3266952dffa11d735b12f1cf008e77677504eb4f52c69947cfe85d87de2c7a8f','2026-10-29 11:39:13.692087','2026-10-29 12:09:13.996409','2026-10-29 11:39:21.172985','2026-10-29 11:39:13.996409',0),
(113,X'210a5a615382ec2651e223692897c0fb5f90e4d98f87fba1c7de53f360cd6619','2026-10-29 11:39:22.268326','2026-10-29 12:12:32.234941','2026-10-29 11:42:32.813677','2026-10-29 11:42:32.234941',0),
(114,X'0a0b7cd1b0bde6638de04bc8c81272a759c9649bb69423e48832a0feafa154c0','2026-10-29 11:42:33.830178','2026-10-29 12:22:13.717182','2026-10-29 11:52:20.075804','2026-10-29 11:52:13.717182',0),
(115,X'b52c171e5283250ca218e4c6527d18b00ea451c66777dda78b3e3a200e2e4f6a','2026-10-29 13:56:13.181165','2026-10-29 14:26:13.647485','2026-10-29 13:56:22.936370','2026-10-29 13:56:13.647485',0),
(116,X'4c7b6ae7a2227eaf04004f4a11bedba1f690417e84f2d178add6a6eb49d115d5','2026-10-29 13:56:24.041997','2026-10-29 14:26:24.360590','2026-10-29 13:56:29.236463','2026-10-29 13:56:24.360590',0),
(117,X'4b7d835e135d8555bf6bad24a6be3e5bc735f7978d8180931cbdbf9389bb4a10','2026-10-29 13:58:25.056524','2026-10-29 14:28:52.187702','2026-10-29 13:58:57.140815','2026-10-29 13:58:52.187702',0),
(118,X'7c0d5419aa27d2b2e43021f5a1855daf05164b9d908d23ce15d7435beec5fa2e','2026-10-29 14:08:14.850066','2026-10-29 14:38:15.407980','2026-10-29 14:08:21.392530','2026-10-29 14:08:15.407980',0),
(119,X'39f4d7a5d1adc32a59d1b99dc532d04913080d80d27e7f2e9eb1e219a7826826','2026-10-29 14:17:44.698527','2026-10-29 14:47:53.998331','2026-10-29 14:18:00.315335','2026-10-29 14:17:53.998331',0),
(120,X'6785c7cff8cd62e5b81ca73aa36d1f549b463b525b77117d38314e4b44e3ee4d','2026-10-29 14:59:49.907262','2026-10-29 15:29:50.394343','2026-10-29 14:59:55.230589','2026-10-29 14:59:50.394343',0),
(121,X'94d17261841204f64976ef054e058573a088f4da301906d81436ebb4227a7bf0','2026-10-29 15:04:10.387023','2026-10-29 15:35:29.608113','2026-10-29 15:05:30.303336','2026-10-29 15:05:29.608113',0),
(122,X'ef039aee4de18f85dfad7df1f18115e5f9becf1d5d86960fecffdcee50ea0449','2026-10-29 16:45:42.888340','2026-10-29 17:29:30.970149','2026-10-29 16:59:31.933358','2026-10-29 16:59:30.970149',0),
(123,X'fadeb1685fcf060b00bc3db28a45c8d39493f10d24f463a5f808df9cad33adbf','2026-10-29 16:59:33.156085','2026-10-29 17:38:58.305014','2026-10-29 17:09:04.677869','2026-10-29 17:08:58.305014',0),
(124,X'9b0daeabb4a2f8c16ac5f3962b6e37944e26f17d437be2a973298c8f8b90578c','2026-10-29 17:09:06.144343','2026-10-29 17:50:04.532124','2026-10-29 17:20:06.803430','2026-10-29 17:20:04.532124',0),
(125,X'77c81d3391ed9f4da8b0719eadfa5c6a48eea17a72cb5887aea8ee63dc5148ee','2026-10-29 17:20:08.136109','2026-10-29 17:50:14.960587','2026-10-29 17:20:21.715250','2026-10-29 17:20:14.960587',0),
(126,X'77763e6f7e5e6086a2222514bde737c04d317b5c21fe8aed776f4e4657a22815','2026-10-29 17:20:23.209318','2026-10-29 17:53:34.739872','2026-10-29 17:23:35.906419','2026-10-29 17:23:34.739872',0),
(127,X'5094a73b084d8ad98f19ac21eb9211377ecc048533bff570fc27dfa37fbcc7ba','2026-10-29 17:23:37.337334','2026-10-29 17:59:57.061147','2026-10-29 17:29:58.267621','2026-10-29 17:29:57.061147',0),
(128,X'e3c90d7a37ef774a1df94c1df3491673583c4832270e8af8b40bcc1ed659081a','2026-10-29 17:29:59.363697','2026-10-29 18:03:39.041749','2026-10-29 17:33:39.055491','2026-10-29 17:33:39.041749',0),
(129,X'08c1f7bae42fe2d438adb942bcaa290875552c1a0b7b419bfb43db853cf61e35','2026-10-29 17:30:30.138817','2026-10-29 18:00:30.432047','2026-10-29 17:30:36.707296','2026-10-29 17:30:30.432047',0),
(130,X'dab2fd8dd0c9617d0280fd6c7f1561afde831c76497ddeb0ca8fb130c9ce4407','2026-10-29 17:30:38.029390','2026-10-29 18:00:38.901085','2026-10-29 17:30:44.646527','2026-10-29 17:30:38.901085',0),
(131,X'15a87a0188f677817e9d26836c2b162d90c1f98006fbe9e60989b0ea7ce1c159','2026-10-29 17:30:45.690451','2026-10-29 18:00:49.509762','2026-10-29 17:30:54.840660','2026-10-29 17:30:49.509762',0),
(132,X'469a5bcbd69d25f147b43a0582d6f232d3bbf8b98d324ef4c5211f6ceca3702b','2026-10-30 00:19:24.667614','2026-10-30 00:49:44.695193','2026-10-30 00:19:46.162108','2026-10-30 00:19:44.695193',0),
(133,X'5d0e3f2145e469f790339194234eae33af88113126f68bdf0ddfbeae8e6e3619','2026-10-30 00:23:07.623949','2026-10-30 00:55:18.551203','2026-10-30 00:25:27.641130','2026-10-30 00:25:18.551203',0),
(134,X'15854ef2f7704edfb1194ba4ce46867b9353509f9f04e53febdf80b3728215c2','2026-10-30 00:23:08.155017','2026-10-30 00:55:16.899887','2026-10-30 00:25:18.508552','2026-10-30 00:25:16.899887',0),
(135,X'658a93fb2c1705227054958a8de0df590db93d6718ddce1df01425153e075480','2026-10-30 00:25:29.002200','2026-10-30 00:57:29.883729','2026-10-30 00:27:33.223224','2026-10-30 00:27:29.883729',0),
(136,X'd99b1c9128eca1c4aa4cc4f6f5dd77651c59db9345b8ea985349234309c3869a','2026-10-30 00:26:12.024550','2026-10-30 00:56:12.024550',NULL,'2026-10-30 00:26:12.024550',0),
(137,X'0ba9cc7ae1e2b112e830f0b275b03c1cdf16ae3b908c25e11ddd1c1cf28e14a5','2026-10-30 00:27:34.800035','2026-10-30 01:03:14.305531','2026-10-30 00:33:16.172324','2026-10-30 00:33:14.305531',0),
(138,X'56672576ddbbc764a970818b413c9e6754a0591e3e7356ea106826b63986fb2c','2026-10-30 00:33:50.851321','2026-10-30 01:04:05.281328','2026-10-30 00:34:10.159585','2026-10-30 00:34:05.281328',0),
(139,X'b8ed5bacb7602f729dbfdf20049b922fcdb372ecba43093704be92c6b9728cd6','2026-10-30 00:34:11.530984','2026-10-30 01:04:21.166256','2026-10-30 00:34:23.030637','2026-10-30 00:34:21.166256',0),
(140,X'50f5bd10fdad5dd2ca9529ab165471973508da22f5a81905cd75dfac83f5175b','2026-10-30 00:35:38.967397','2026-10-30 01:05:56.917793','2026-10-30 00:36:03.446396','2026-10-30 00:35:56.917793',0),
(141,X'e906237c18ef171b6200768079230da6932183a1e773147a27fc6223f35320e3','2026-10-30 00:36:00.816254','2026-10-30 01:12:39.314328','2026-10-30 00:42:43.475891','2026-10-30 00:42:39.314328',0),
(142,X'da0d388671303e1e590393afd282ba84f4caeb08ba46b03568f4a3b0f6d8ac19','2026-10-30 00:39:49.543753','2026-10-30 01:09:50.079316',NULL,'2026-10-30 00:39:50.079316',0),
(143,X'a3bcfc09b9c1ea5ea8a6375d75ed45bdf8c61bb52e3eedc9cc583a5ffc95baed','2026-10-30 00:42:44.834347','2026-10-30 01:13:02.098422','2026-10-30 00:43:05.588960','2026-10-30 00:43:02.098422',0),
(144,X'3a2020d693205cd93171ccc4d9ec385063517e1a886b2e1ce229737ec3f7502c','2026-10-30 00:55:21.453166','2026-10-30 02:06:51.487867',NULL,'2026-10-30 01:36:51.487867',0),
(145,X'a46dea8ff4737de5b54361729a5e5dd842880be134a5ac0fa0cbde323f6f7cb5','2026-10-30 00:58:33.560376','2026-10-30 01:28:34.123767',NULL,'2026-10-30 00:58:34.123767',0),
(146,X'c1841a3b2279400ae4c5ccaf3d19e9b29f5eb7098b896933a2fe92600adb9e30','2026-10-30 01:00:22.633699','2026-10-30 02:07:02.966079',NULL,'2026-10-30 01:37:02.966079',0),
(147,X'1a3bd4f8c9390c1b7d5fd1973774a247e56472081002e77e8c8a4e7ece267227','2026-10-30 01:01:02.727348','2026-10-30 01:31:03.176809','2026-10-30 01:01:06.631435','2026-10-30 01:01:03.176809',0),
(148,X'59f5fdec37be9b6cff6532e0dba216ed6cd5c3d1cf84b13dc30fc146ba80dde4','2026-10-30 01:01:07.864239','2026-10-30 01:31:08.185021','2026-10-30 01:01:16.569340','2026-10-30 01:01:08.185021',0),
(149,X'c6993b62defacfc9d23f3a9930ddbb5d6fb28bbde587a4e363e7ab46ae388c29','2026-10-30 01:01:08.097367','2026-10-30 01:31:18.140124','2026-10-30 01:01:24.547334','2026-10-30 01:01:18.140124',0),
(150,X'a41ce23b446949db28fcf055b73e7b251afecdbc016f1e3d1b6ee9abe13ac18b','2026-10-30 01:01:18.228148','2026-10-30 01:41:56.837410','2026-10-30 01:12:03.013075','2026-10-30 01:11:56.837410',0),
(151,X'cb896693a34170be56b074989cb0f622ef569d309a4f3c294d4a88653b62cda3','2026-10-30 01:41:30.777334','2026-10-30 02:14:53.634889',NULL,'2026-10-30 01:44:53.634889',0),
(152,X'58438dcd21640bafcd8f7f3484699ddc5eb19ebaad3787ab48e996ac0f8af2be','2026-10-30 01:47:02.064359','2026-10-30 02:25:46.592396','2026-10-30 01:55:51.434419','2026-10-30 01:55:46.592396',0),
(153,X'34b9831f8e9bef80cd89f44dea84f26ba844e8881424d223e72d0ffc5a94cf5e','2026-10-30 01:48:16.931527','2026-10-30 02:18:17.403941','2026-10-30 01:48:24.171602','2026-10-30 01:48:17.403941',0),
(154,X'7b9e45f07c07db36c9debacadedc4f6ec820a9dc316f9ef2f1d60b7e19c80268','2026-10-30 01:55:52.812179','2026-10-30 02:25:53.299822',NULL,'2026-10-30 01:55:53.299822',0),
(155,X'ff26524e65a01370bc91f8aacaa152d9097269baadc31dd3f39ce619b6afbb0b','2026-10-30 01:56:25.997932','2026-10-30 02:26:25.997932',NULL,'2026-10-30 01:56:25.997932',0),
(156,X'02a756b7acddbb6f6d948b027a9cd63e9f315f9985f09eb0ebdfde856230059a','2026-10-30 01:59:29.070253','2026-10-30 02:29:29.104049',NULL,'2026-10-30 01:59:29.104049',0),
(157,X'4cc11c2243889d2b531b767859ece74161cfd683dbdbdd237119e8b8be662964','2026-10-30 02:01:00.355508','2026-10-30 02:31:00.785611',NULL,'2026-10-30 02:01:00.785611',0),
(158,X'583d950ce5fb329c9b2369ec91ed21c20f85f0c8ca93549a44f55cd255076c99','2026-10-30 02:01:47.522405','2026-10-30 02:31:47.556695',NULL,'2026-10-30 02:01:47.556695',1),
(159,X'aeb1a724294dd487159cee4fc993a6224cf44a4ae1ae4aae72bff4a72298b571','2026-10-30 02:02:56.891486','2026-10-30 02:36:20.438238',NULL,'2026-10-30 02:06:20.438238',0),
(160,X'559ff578b895a0a16a88d7a74d9bb2d9f689d05751b614836b5d630972719eb6','2026-10-30 02:03:07.804135','2026-10-30 02:33:33.623891',NULL,'2026-10-30 02:03:33.623891',0),
(161,X'70ade12cdd1e26aff2f06f4b5a5ce27cc623b4c622d9826367b4f7bfcd1a1598','2026-10-30 02:04:26.150307','2026-10-30 02:37:46.892170',NULL,'2026-10-30 02:07:46.892170',0),
(162,X'44886af7dc8606d12cd6ac514c94b7e270ba25ceb700dd774fb4b3d224939fee','2026-10-30 02:04:53.857407','2026-10-30 02:34:54.250818',NULL,'2026-10-30 02:04:54.250818',0),
(163,X'161b9d3631d592a4ede9a4b5cebc00ba7fce72f18b4389e564ba199669457e75','2026-10-30 02:04:59.167114','2026-10-30 02:38:49.158049',NULL,'2026-10-30 02:08:49.158049',0),
(164,X'3e952ad8e6ca3d89b838589bed091db43d8621091fc18ff853c72f842e77b4d4','2026-10-30 02:49:04.456296','2026-10-30 03:35:34.980195',NULL,'2026-10-30 03:05:34.980195',0),
(165,X'7dbe9e1394152de1861c7bf9eba01a21bb6bb52f770f12b9d1e03320112736d6','2026-10-30 02:58:51.080306','2026-10-30 04:02:30.079952','2026-10-30 03:32:37.130330','2026-10-30 03:32:30.079952',0),
(166,X'ac1f03e36a97710365ef3ee1a2ae105474892715770e3994d4f6467bc47091c4','2026-10-30 02:58:54.955922','2026-10-30 03:28:55.249530','2026-10-30 02:59:05.216953','2026-10-30 02:58:55.249530',0),
(167,X'de8630df99a06dec505c07bdf7cd199e2635d896943e322e72d4a0de14d73804','2026-10-30 02:58:57.158015','2026-10-30 03:30:58.552991','2026-10-30 03:01:06.461331','2026-10-30 03:00:58.552991',0),
(168,X'fbbfd539f275ad8f19b602dc1f65ea8fefca17ac6bcaf4749c5cc38612c01f50','2026-10-30 02:59:06.568430','2026-10-30 03:29:40.849364','2026-10-30 02:59:46.773561','2026-10-30 02:59:40.849364',0),
(169,X'99d12ab05e1c6742ad3566cfceb8d82f6c0b86d63298e07456a4bd858412ab7b','2026-10-30 02:59:48.174332','2026-10-30 03:40:46.127731',NULL,'2026-10-30 03:10:46.127731',0),
(170,X'a833857d3c6ea5999f40324aeb752379ee6cb46ebb07167afb4bae2d28cf0a06','2026-10-30 03:01:07.490688','2026-10-30 03:31:09.209134','2026-10-30 03:01:12.437444','2026-10-30 03:01:09.209134',0),
(171,X'89356e6040034a292453844ee71852792677012ffc73c0ca2efdd26aeaedcabc','2026-10-30 03:01:13.461940','2026-10-30 03:32:23.288292','2026-10-30 03:02:27.236246','2026-10-30 03:02:23.288292',0),
(172,X'31c4a485737d615789458fd6331700226673f1aecd031da8a8bb70cce00f3fa0','2026-10-30 03:02:28.297971','2026-10-30 03:34:05.575305','2026-10-30 03:04:10.282929','2026-10-30 03:04:05.575305',0),
(173,X'b0698bfe957bde66f853935c9749671b51b121fb17c9ad07de2a1d330aec5e7e','2026-10-30 03:04:11.309776','2026-10-30 03:34:11.668451',NULL,'2026-10-30 03:04:11.668451',0),
(174,X'5a8d58d63156196f914e25869a6a01fb827d0361c860e81330de82c6c9bfbd0d','2026-10-30 03:11:02.129838','2026-10-30 03:41:33.016694',NULL,'2026-10-30 03:11:33.016694',0),
(175,X'04857dd0d2afc2201b7832feb842e045746fe2a49e3b99891cc61dc1a81880b2','2026-10-30 03:21:24.694185','2026-10-30 03:51:24.971767',NULL,'2026-10-30 03:21:24.971767',0),
(176,X'40fdcee6d9bb775c07e8164c19cb6f5bc4583fa9a5ed8fb2fbdd62f13ea40b5d','2026-10-30 03:30:36.945759','2026-10-30 04:00:46.950248',NULL,'2026-10-30 03:30:46.950248',0),
(177,X'6b3a37ceb6b9b9992ea76710ab182104f587997ddb8f18dbea8df07ca64f26e3','2026-10-30 03:32:19.191052','2026-10-30 04:02:19.665699',NULL,'2026-10-30 03:32:19.665699',0),
(178,X'7ec149a0b9acd43400b8eee84233be6756fc537ca132b9c6a04dbea8d746165b','2026-10-30 03:35:48.727809','2026-10-30 04:15:48.053136','2026-10-30 03:45:49.262331','2026-10-30 03:45:48.053136',0),
(179,X'f55a53657f5146346ef9f0ff84dd7094f68dd20b38fbad2c86af14bedba59942','2026-10-30 03:38:45.067339','2026-10-30 04:08:50.364496',NULL,'2026-10-30 03:38:50.364496',0),
(180,X'8291c2bd37f62beed51ea5ed3b12cf1bd90bb47a2d499d503c1ee19320d72d1c','2026-10-30 06:13:01.865867','2026-10-30 06:43:08.060516','2026-10-30 06:13:10.263549','2026-10-30 06:13:08.060516',0),
(181,X'179fe40c0444181c42ed3d3d317252efffa6fa73fb45453516b77c33d4b1b49e','2026-11-02 09:04:45.914729','2026-11-02 09:42:40.034056',NULL,'2026-11-02 09:12:40.034056',0),
(182,X'20b6ef409f99e2bbb234ca1b0c08418009ce5649c803ad90598d1bb387c364ec','2026-11-02 09:14:29.625457','2026-11-02 09:44:29.906040','2026-11-02 09:14:32.547698','2026-11-02 09:14:29.906040',0),
(183,X'f3193fa147508940a0cc262bb6f9714bc7c45152168bcd578b21835c3b92498c','2026-11-03 15:44:53.793278','2026-11-03 16:15:00.136107',NULL,'2026-11-03 15:45:00.136107',0),
(184,X'947e04b12e2c7c4572dec991f5ec85ea11b29dc4790ea7ed5a05b78341fbcc1d','2026-11-03 15:45:11.800844','2026-11-03 21:29:32.936493',NULL,'2026-11-03 20:59:32.936493',0),
(185,X'59da9181263a15738c26000a2286e930f36f91d34bb44ae675b183a445ef5756','2026-11-04 08:04:38.322471','2026-11-04 08:34:38.775492',NULL,'2026-11-04 08:04:38.775492',0),
(186,X'52bbd491baaa0dd215408221a19752a3e1b78791b1a7829dde1102b8685d6bf6','2026-11-04 13:33:30.481005','2026-11-04 14:03:31.144052',NULL,'2026-11-04 13:33:31.144052',0),
(187,X'af1cd85e8944a790c0a40781f0318eea65eefc7b4ff8f5d3068ee6e4b19189d1','2026-11-04 14:52:15.738892','2026-11-04 15:22:34.035458','2026-11-04 14:52:35.127592','2026-11-04 14:52:34.035458',0),
(188,X'a6aac20186d34e0169e0dcb08cdb8dab45719e5267a7b16bf2e843bfcb6bb085','2026-11-04 14:52:35.445590','2026-11-04 15:23:41.036857',NULL,'2026-11-04 14:53:41.036857',0),
(189,X'fd34ebb266608d384ff50a3ad0b077f5feebb499bf28e2217b41ae3c63d8b3da','2026-11-04 18:55:07.774447','2026-11-04 19:25:08.041853',NULL,'2026-11-04 18:55:08.041853',0),
(190,X'4d43cdc511c11e37b683e56920d0969374ab8236d46f086ec5c50d0b9c55fd39','2026-11-05 07:10:35.447454','2026-11-05 07:46:56.748592',NULL,'2026-11-05 07:16:56.748592',0),
(191,X'ccb7a5dc3498782e5f0e2dee308be31e475a9481a8f5a31eb7892d1ef2ad6bb5','2026-11-05 07:10:46.828950','2026-11-05 07:40:52.206211',NULL,'2026-11-05 07:10:52.206211',0),
(192,X'09d154202d53ab60369b1d233e51cdc855cee681271b1d16c116a840420f24f6','2026-11-05 07:10:53.507953','2026-11-05 07:40:58.780889',NULL,'2026-11-05 07:10:58.780889',0),
(193,X'4213f154d10c1f94a5e5c6f36a5bbe6df5227d3319923a90ffcd74fbe74ddfbf','2026-11-05 07:11:13.064130','2026-11-05 07:41:18.820661',NULL,'2026-11-05 07:11:18.820661',0),
(194,X'45c8089208a373f6c3249abf6b6ace33d07de88f43994de405fab0d198ee5e13','2026-11-05 07:11:55.020113','2026-11-05 07:42:00.392526',NULL,'2026-11-05 07:12:00.392526',0),
(195,X'307413405f7662d74eec207df9106845fd9204350cc5806ef2ad321a33349a1a','2026-11-05 09:27:52.484825','2026-11-05 09:58:54.276735',NULL,'2026-11-05 09:28:54.276735',0),
(196,X'77180afc6e5d8620f8789774c369acec98aab019e70ec359878a344df5ed9ddf','2026-11-05 09:57:01.089667','2026-11-05 10:27:07.093052',NULL,'2026-11-05 09:57:07.093052',0),
(197,X'3cc39fb4b4f60c0097db1216328ab94e5f9f35492e899ed2a102f5a87c449c93','2026-11-05 09:57:09.820179','2026-11-05 10:27:12.826088','2026-11-05 09:57:14.563773','2026-11-05 09:57:12.826088',0),
(198,X'201dcc1b161666a48baca4ff23747f6cadec7b27ca80046014a94f315f9d1472','2026-11-05 09:57:16.045358','2026-11-05 10:27:39.918495','2026-11-05 09:57:39.951452','2026-11-05 09:57:39.918495',0),
(199,X'73934746eb4448294619a89fc322e8bb06bfde3954dc12667eb4e5ef9bd5c46b','2026-11-05 12:53:51.471074','2026-11-05 13:23:51.885241',NULL,'2026-11-05 12:53:51.885241',0),
(200,X'1ba72a290f50daf4083fbbd924b3cfde20dec3d174257c9e74c22b714630e0c1','2026-11-05 14:06:43.825491','2026-11-05 14:38:39.496624','2026-11-05 14:08:41.242841','2026-11-05 14:08:39.496624',0),
(201,X'51c6653b6b300f6582fabf8d6a427201752f51a3dcdba508730460a569baf474','2026-11-05 14:08:42.815453','2026-11-05 14:38:45.450837','2026-11-05 14:08:45.733226','2026-11-05 14:08:45.450837',0),
(202,X'c89479ee4c60d6fc4303107f0d9cea5ec4c3c341d16056c46bee13f0a20fb915','2026-11-05 14:08:47.618191','2026-11-05 14:38:47.618191',NULL,'2026-11-05 14:08:47.618191',0),
(203,X'de78e7effacaf1532af76cc4b3c8659adb95d3679613d3d7d0506913a8c4663e','2026-11-05 14:08:48.658811','2026-11-05 14:39:28.706736','2026-11-05 14:09:28.807891','2026-11-05 14:09:28.706736',0),
(204,X'bd8651b1b7d473c5db21f057822c59676538d6ae8c0297241119cd373793bc62','2026-11-05 14:09:30.319322','2026-11-05 14:41:11.460394',NULL,'2026-11-05 14:11:11.460394',0),
(205,X'c6174cdcf81103430a2cfe0700596dcbe6b5fe3a6343e7c4a351a5b138d911c8','2026-11-05 15:12:22.282655','2026-11-05 15:42:40.825096',NULL,'2026-11-05 15:12:40.825096',0),
(206,X'ceb9c8da2be934f701cde8f0caae78eb2b69edca8d354002fbfb3813eb0d11ec','2026-11-05 15:49:31.855538','2026-11-05 16:19:34.617750','2026-11-05 15:49:37.742406','2026-11-05 15:49:34.617750',0),
(207,X'e978218e730fcc8708e8a6ad32546d60fc6989901eae1c47a99954d5eaeb69d8','2026-11-05 17:40:51.211816','2026-11-05 18:10:51.584497',NULL,'2026-11-05 17:40:51.584497',0),
(208,X'fd72166a25e87b0785c18ae71e6e0c3ba9b67074f8dc5d16bc58c75686dba003','2026-11-05 21:38:19.141240','2026-11-05 22:08:19.520456','2026-11-05 21:38:21.876289','2026-11-05 21:38:19.520456',0),
(209,X'650aaf246001d043c438273679e5f6b7b0af5cfbb8563517857196328db38177','2026-11-06 08:17:06.838337','2026-11-06 08:47:06.838337',NULL,'2026-11-06 08:17:06.838337',0),
(210,X'a0ee84a84bd3c8cfc6bf37dab39a619046c3b54d5cfc4816d98ed2a31c8588db','2026-11-06 08:17:15.187851','2026-11-06 08:48:18.906787','2026-11-06 08:18:20.383999','2026-11-06 08:18:18.906787',0),
(211,X'b8ff5078fb6c3cf225db288e2d68ebf4b1e857fac18d7fbb5904d1fb4630c0bf','2026-11-06 08:19:12.345928','2026-11-06 08:49:21.313212',NULL,'2026-11-06 08:19:21.313212',0),
(212,X'766a26ac03b5ca2830ef0d4158d5c5ece671a72504ca223e1b7a2257aa5f7418','2026-11-06 21:47:58.270200','2026-11-06 23:18:25.782442','2026-11-06 22:48:27.275650','2026-11-06 22:48:25.782442',0),
(213,X'caa7ca78cc59d3cb2a69cdb77409356ff5a5487e68597246416fd535da0534ba','2026-11-06 21:48:06.777386','2026-11-06 22:18:46.384887','2026-11-06 21:48:47.473021','2026-11-06 21:48:46.384887',0),
(214,X'348432776e3d661f5ab705db17d88e04aeb196f1721e4610dbd94c567bf576a1','2026-11-06 21:48:48.251294','2026-11-06 22:18:48.514260','2026-11-06 21:48:49.594046','2026-11-06 21:48:48.514260',0),
(215,X'b08940f907644732ead7e73dca07ef1ae93ecccdca2722b267a097185217735e','2026-11-06 21:48:50.559718','2026-11-06 22:23:44.311163','2026-11-06 21:53:45.280591','2026-11-06 21:53:44.311163',0),
(216,X'89177eba2f5969954edc584407a132f66c51174b75e45a0e05b1c944bac2152c','2026-11-06 22:46:27.048015','2026-11-06 23:16:39.375434','2026-11-06 22:46:40.847464','2026-11-06 22:46:39.375434',0),
(217,X'2599b7f5e9697eed4256f702b92c376ab3560ec451afe146faf1f0627e0fa8d1','2026-11-06 22:49:55.072443','2026-11-06 23:20:00.826677',NULL,'2026-11-06 22:50:00.826677',0),
(218,X'b407633a3f2d56467da11307369bc1413e5c1f7c5163edc575c3cfed94044b0d','2026-11-06 23:22:22.615442','2026-11-06 23:52:23.821412',NULL,'2026-11-06 23:22:23.821412',0),
(219,X'3ad7c3b9347e610f58f5fee018516f4b651b98491a85824c77e406ad628387a0','2026-11-06 23:22:23.295274','2026-11-06 23:52:24.104241',NULL,'2026-11-06 23:22:24.104241',0),
(220,X'413905708f9f415ad5c8c3c30af6093055d77a9288c9f7595ce7cf160c6ccdc1','2026-11-06 23:22:26.181108','2026-11-06 23:52:28.466706',NULL,'2026-11-06 23:22:28.466706',0),
(221,X'd7683eda88ad199cb0fa1585bd0332d8b70685ccfacca5a58cf506ec3586c923','2026-11-07 06:19:10.608534','2026-11-07 06:51:13.124290','2026-11-07 06:21:15.687120','2026-11-07 06:21:13.124290',0),
(222,X'9ec60f2e3777a335848569958407caf80bba81af32767991afb688c5954ef3cf','2026-11-07 06:21:17.351632','2026-11-07 11:26:57.334342','2026-11-07 10:56:58.355601','2026-11-07 10:56:57.334342',0),
(223,X'823524c23c50598b35bcf6943fd96e4e56ed1218fcbc9e43a07fb0293823e4be','2026-11-07 10:55:07.637440','2026-11-07 11:39:30.724586','2026-11-07 11:09:31.630045','2026-11-07 11:09:30.724586',0),
(224,X'87b369fb56b42fcf8c33566bbcc457c18fd8857bd2a9783fb5535378926dacbe','2026-11-07 10:57:00.932481','2026-11-07 11:27:20.532407','2026-11-07 10:57:22.247587','2026-11-07 10:57:20.532407',0),
(225,X'81e336a5b5b5fbcf261a600238f82f014029f88280213d68d1a9e29dd618269d','2026-11-07 10:57:23.677053','2026-11-07 12:47:53.901142','2026-11-07 12:17:54.850576','2026-11-07 12:17:53.901142',0),
(226,X'9ef33eb3a4d59c1c9c827953572eae9b2ba8dcfb046e4dfdf7eb4dad79b8c246','2026-11-07 12:17:56.852464','2026-11-07 12:47:59.418533',NULL,'2026-11-07 12:17:59.418533',0),
(227,X'7ab3e19c44c7a6857d80de466a76cc4885a9f3e158dbb5b79be4f7f2fb308da7','2026-11-07 12:19:40.397449','2026-11-07 12:49:40.397449',NULL,'2026-11-07 12:19:40.397449',0),
(228,X'cc8995e01a35a71486aa8a528fb7f3ac4058c27b8a857feabc2d7b20dc2cc111','2026-11-07 12:19:43.724351','2026-11-07 12:58:14.209691','2026-11-07 12:28:16.377716','2026-11-07 12:28:14.209691',0),
(229,X'5e01a131814f1730934c25ca5294663e9f640c28552cea74526004e7e6423835','2026-11-07 12:31:03.451224','2026-11-07 13:01:04.274014','2026-11-07 12:31:04.612927','2026-11-07 12:31:04.274014',0),
(230,X'a806e75285ddb61e07ea887baff9cdefcb110da44180186a0af784098c8f27b6','2026-11-07 12:31:09.004196','2026-11-07 14:15:57.044367',NULL,'2026-11-07 13:45:57.044367',0),
(231,X'4691ebcfa67ea0dd5b9809a839a17a5d49337e111f32ab2716c855ff1f08ab28','2026-11-07 13:03:08.682238','2026-11-07 13:33:35.458994','2026-11-07 13:03:36.374071','2026-11-07 13:03:35.458994',0),
(232,X'6787576a4b33f0d1b25fa902bfce65304ee7ee82bfb4093f30c28ce3a5557c95','2026-11-07 17:58:01.594593','2026-11-07 18:28:14.100388',NULL,'2026-11-07 17:58:14.100388',0),
(233,X'70047007ead6385e808e134908f2af469c4c79534f12169d648ca182deb218ed','2026-11-10 00:36:17.382547','2026-11-10 01:06:34.656438','2026-11-10 00:36:35.578446','2026-11-10 00:36:34.656438',0),
(234,X'f3a92842aabbb6d8cd519d04111d51cf29c1e003a0f0c69d9009d0f13c86d30a','2026-11-10 00:36:36.888909','2026-11-10 01:06:39.002463','2026-11-10 00:36:40.347865','2026-11-10 00:36:39.002463',0),
(235,X'fe1b8ceafe5d36858a8a2ebde839e96003b352180d970ba09a67f75bbdd988ff','2026-11-10 00:36:41.807310','2026-11-10 01:06:43.648320','2026-11-10 00:36:45.319061','2026-11-10 00:36:43.648320',0),
(236,X'09486a213d12b43a529e2147557d1ce2532efe5e10989fc8af8e40aef3cff9cf','2026-11-10 00:36:50.722491','2026-11-10 01:06:52.718512','2026-11-10 00:36:54.504443','2026-11-10 00:36:52.718512',0),
(237,X'bf630a40acbb369e8bd41b4204db81347338502b5701cf62c0497b3b128eb696','2026-11-10 00:36:57.207443','2026-11-10 01:06:57.207443',NULL,'2026-11-10 00:36:57.207443',0),
(238,X'fbe66bb89b246cbcfa4fe0df6d69f9d888941dcffa5eb165f29afa3a179245fd','2026-11-10 00:37:04.205795','2026-11-10 01:07:11.557664','2026-11-10 00:37:11.866021','2026-11-10 00:37:11.557664',0),
(239,X'5afcd9dd4a7b8b6a55ffbd0c59292cbda816df74221408c213536e5af2017354','2026-11-10 00:37:12.475126','2026-11-10 01:07:30.215472','2026-11-10 00:37:32.638653','2026-11-10 00:37:30.215472',0),
(240,X'f3cd8d20cf28766d908da45fa1db3a49d5405c598848bb9807a7a8beba7c6886','2026-11-10 00:37:17.756803','2026-11-10 01:07:23.393734',NULL,'2026-11-10 00:37:23.393734',0),
(241,X'54be7edf7cba002a737f35d4f198d37216ee04d0577c04c0339e87f3d4f56e44','2026-11-10 00:37:34.034711','2026-11-10 01:07:39.809293','2026-11-10 00:37:41.510017','2026-11-10 00:37:39.809293',0),
(242,X'8754970ea0f735e28328294cbe0dc2398a72d284c63b9eb8c3d932ec825b4871','2026-11-10 01:03:22.554687','2026-11-10 01:33:32.730575','2026-11-10 01:03:35.193745','2026-11-10 01:03:32.730575',0),
(243,X'b7c4291599e221975e31ab2e535555253e556e02eedcd20c3f9f932350898b5b','2026-11-10 01:03:36.752721','2026-11-10 01:33:41.266745','2026-11-10 01:03:44.061847','2026-11-10 01:03:41.266745',0),
(244,X'28834c671252fff541371e63464a3a575f097862c3e774b389e1e54083c3e75c','2026-11-10 01:03:45.265776','2026-11-10 01:35:23.268482','2026-11-10 01:05:24.571861','2026-11-10 01:05:23.268482',0),
(245,X'67de22c85a39db81b82e772d48f6b269f64bb560390455ce7e54f8b78a5f0b54','2026-11-10 01:05:25.432866','2026-11-10 01:36:04.051221','2026-11-10 01:06:05.742759','2026-11-10 01:06:04.051221',0),
(246,X'74d732f96016ad17ee406ed0560ef4d429cec3a778b0d303a9344316482068d2','2026-11-11 08:22:49.801953','2026-11-11 08:53:06.923336',NULL,'2026-11-11 08:23:06.923336',0),
(247,X'497ac8b97a9a6b38747f4491853c452882aa2f228e4347b06b40f88fe6934ca1','2026-11-13 02:28:03.532146','2026-11-13 03:07:20.497551','2026-11-13 02:37:20.537459','2026-11-13 02:37:20.497551',0),
(248,X'0aa5551e26ad6e27d51e2a32e88a996129408d6868b261fd1778a3c3d4523663','2026-11-13 02:35:10.477006','2026-11-13 03:05:16.436272',NULL,'2026-11-13 02:35:16.436272',0),
(249,X'a98c8f8a717672cf3caf1f0efd2ad181eaaf1a1b76266abbe96421c4f9e6f480','2026-11-13 02:37:55.792785','2026-11-13 03:07:56.267582','2026-11-13 02:37:58.164595','2026-11-13 02:37:56.267582',0),
(250,X'3dc6dcfc2b604937d1e550d9e7c6090f61d66ef0e783297862cddc5454e95209','2026-11-13 02:40:20.584814','2026-11-13 03:10:25.870942',NULL,'2026-11-13 02:40:25.870942',0),
(251,X'b9d9d6c5df3935508caf1a81ac751196de8c7d32e67140dba236be65d9bab959','2026-11-13 16:16:04.270813','2026-11-13 16:46:30.884768',NULL,'2026-11-13 16:16:30.884768',0),
(252,X'246a1e726109122e6d13a5310331ed967ce599a8de7a2f890771c2889b1d1112','2026-11-13 16:16:29.575882','2026-11-13 16:46:56.790960','2026-11-13 16:16:59.949075','2026-11-13 16:16:56.790960',0),
(253,X'd35a4c3ca68f8627948507c4650862f8bf8147217705fdab6d15ed2f5d522dfb','2026-11-13 16:17:37.976127','2026-11-13 16:47:40.980861',NULL,'2026-11-13 16:17:40.980861',0),
(254,X'a0451a8b4e998938056a1955a7b0deaf28c0ca1d54cb8ec9d2c88a991085e155','2026-11-13 21:04:08.086005','2026-11-13 21:34:14.091553','2026-11-13 21:04:14.883340','2026-11-13 21:04:14.091553',0),
(255,X'b17bfb77875f64752077bc6b771c9fa32d9b887daed4253609cb2064b7419ea3','2026-11-14 00:45:27.300578','2026-11-14 01:15:39.080446','2026-11-14 00:45:40.123134','2026-11-14 00:45:39.080446',0),
(256,X'61f5c485ebe9e2da2851a78e7297bd7b66f07dd90abe684fdb2f4c00cdce228a','2026-11-14 00:45:29.192879','2026-11-14 01:15:29.450207','2026-11-14 00:45:31.001837','2026-11-14 00:45:29.450207',0),
(257,X'953909d691a28da2b81399fb347813a14bee07622a82d9f15d973e161b55a861','2026-11-14 01:23:41.069345','2026-11-14 01:53:46.591622',NULL,'2026-11-14 01:23:46.591622',0),
(258,X'c26ea1a496b13d20aef1f7b4596d31b9fe6a26f2cacb74acdf98ccf1f4806ff6','2026-11-14 01:52:55.584975','2026-11-14 02:23:01.130591',NULL,'2026-11-14 01:53:01.130591',0),
(259,X'ec5a1ee03fccb2e24fdf69c562555d86e41fa0aead1bf97cb825605cf46cfd77','2026-11-14 13:17:55.516067','2026-11-14 13:47:59.748454','2026-11-14 13:18:02.309220','2026-11-14 13:17:59.748454',0),
(260,X'c65ecf5c942abbecc0dba17ce149a2fc559ee7b123a88bcd43004b09ac82ff6b','2026-11-14 13:18:06.665098','2026-11-14 14:02:55.281447','2026-11-14 13:32:55.629806','2026-11-14 13:32:55.281447',0),
(261,X'943e485c4e54c7ef48906ea5672aae41e2a81edc745ac917a071bc80f6a1563b','2026-11-14 14:29:00.820157','2026-11-14 16:15:09.126024','2026-11-14 15:45:09.594616','2026-11-14 15:45:09.126024',0),
(262,X'803fc67e73edd4f28054562b58942be8a5f1e8e76260d27a0b6ea6d90fec5b31','2026-11-14 15:58:17.930722','2026-11-14 16:28:17.930722',NULL,'2026-11-14 15:58:17.930722',0),
(263,X'a4e413142f5969ae88a98ff91f322be15a2fbcac235ce256ddb646aa5f7df200','2026-11-14 15:58:19.213447','2026-11-14 16:46:31.471923','2026-11-14 16:16:32.277084','2026-11-14 16:16:31.471923',0),
(264,X'42eafb0212c1b260c8295ffc5ce9ea06fc56658d4b0315dd41c363284d30c74e','2026-11-14 16:16:34.011713','2026-11-14 17:35:49.541584','2026-11-14 17:05:52.365435','2026-11-14 17:05:49.541584',0),
(265,X'71f9594a620ee1cbf512703b1168618686c9e1c26e6bbc023b317ae896ff7f5f','2026-11-14 16:23:25.353467','2026-11-14 16:54:40.360875','2026-11-14 16:24:43.398648','2026-11-14 16:24:40.360875',0),
(266,X'b161bb5880263e3c8b4b4758202c447ef31c0305c4e3d72648d683a655f0e798','2026-11-14 16:24:46.508248','2026-11-14 16:57:38.253631',NULL,'2026-11-14 16:27:38.253631',0),
(267,X'3c529f90517a5707e3dc95256f3eaa9904dd40b71133e206d68c581e783d9513','2026-11-14 16:41:42.310084','2026-11-14 17:11:46.101880','2026-11-14 16:41:47.529438','2026-11-14 16:41:46.101880',0),
(268,X'86c36e70da7e6efdf0e6ef213baca6816532d323765fb3fab73c11a75598289d','2026-11-14 17:03:46.370484','2026-11-14 17:34:26.858230',NULL,'2026-11-14 17:04:26.858230',0),
(269,X'48033cf87eca5c60d333f9efa5b490dddd44528fd1766d2a1f40e8cd49f72e6e','2026-11-14 17:05:54.451407','2026-11-14 17:36:29.545453','2026-11-14 17:06:30.864686','2026-11-14 17:06:29.545453',0),
(270,X'ff344555a6fce1b2f9678774e6e4c605e23ee1c984678a92bda04ac0737b2860','2026-11-15 05:31:41.049902','2026-11-15 06:02:17.065007','2026-11-15 05:32:18.073956','2026-11-15 05:32:17.065007',0),
(271,X'013e63a829134895a17b93beb37d5508d3078a7c063e55d910a8968109dfd1fb','2026-11-15 05:32:18.673265','2026-11-15 06:02:18.957159','2026-11-15 05:32:20.143890','2026-11-15 05:32:18.957159',0),
(272,X'7194da12fbcbfb55e241723c91a0843f8996f36f82364e3d8f25cf03d8f1f891','2026-11-15 05:32:21.094213','2026-11-15 06:02:23.855829','2026-11-15 05:32:24.010365','2026-11-15 05:32:23.855829',0),
(273,X'8d53003a6e36d0481f9edab620b8765ffc246cb1311728e400c38c542f670dcc','2026-11-15 05:32:25.001637','2026-11-15 06:02:33.747253','2026-11-15 05:32:33.979587','2026-11-15 05:32:33.747253',0),
(274,X'15a446845600a3321db7f0b0cd7d386ecac42f78332e272f8545f7dff798721c','2026-11-15 05:32:35.181076','2026-11-15 06:21:58.422428',NULL,'2026-11-15 05:51:58.422428',0),
(275,X'9c9b917578cf0f4547a890bfaacd9a9bd4732f53be40fa463aaefb1943f0ca7c','2026-11-15 05:32:47.160810','2026-11-15 06:02:52.457404',NULL,'2026-11-15 05:32:52.457404',0),
(276,X'506b5a908acb42375da91dd4036e6613a3ab4fad0dde1b470df9881fe96b9d56','2026-11-15 05:58:26.820032','2026-11-15 06:28:32.836912','2026-11-15 05:58:33.255081','2026-11-15 05:58:32.836912',0),
(277,X'b2aa4101540a5fe374cec2f752db287cf253ecef94eb07d002d8bc1d901f70b0','2026-11-15 05:58:34.482873','2026-11-15 06:30:30.010469','2026-11-15 06:00:30.755443','2026-11-15 06:00:30.010469',0),
(278,X'ea04af7c70f452d0c6027fe597600c4308fe1c6436844668e1491c21153117dd','2026-11-15 06:00:32.517049','2026-11-15 06:40:26.072556',NULL,'2026-11-15 06:10:26.072556',0),
(279,X'f9e626d972c2a8cdd650c5ab771a9edcec2fda1a91640f17a72301b480dc9bb8','2026-11-15 06:26:02.902865','2026-11-15 06:56:49.158987',NULL,'2026-11-15 06:26:49.158987',0),
(280,X'2af7c86ba98acaee2719f3b15528150ecd4bb81fe6e38ce7a0ceac75564273d8','2026-11-15 07:36:46.034362','2026-11-15 09:26:44.851459',NULL,'2026-11-15 08:56:44.851459',0),
(281,X'3dd0fc83910d3f5a0175d197a672fdc03c453c65f1c8b5f3eae5c45fcd1a6522','2026-11-15 09:47:20.310490','2026-11-15 11:39:55.606114','2026-11-15 11:10:18.892515','2026-11-15 11:09:55.606114',0),
(282,X'c38901f0f2b19b611e8bdbb37305b09faafcea88bf400ce524b7e93599270206','2026-11-15 15:58:36.356436','2026-11-15 16:28:39.342889',NULL,'2026-11-15 15:58:39.342889',0),
(283,X'695c7baa38fdb352219792da8a92e0397c9e57468a0512ba9d6396c53876ab1c','2026-11-16 09:43:14.990142','2026-11-16 10:30:44.511303',NULL,'2026-11-16 10:00:44.511303',0),
(284,X'a218621fa35b04d1cc7736b9980178a8aa613cb7b714fa3f6dcc8c61b49c3f19','2026-11-16 11:36:10.132034','2026-11-16 18:08:43.214527',NULL,'2026-11-16 17:38:43.214527',0),
(285,X'7f0fed5b3e87c4edfdd3f93402b89b44afa9d7cb28b61c4c18e425d64f7d36b6','2026-11-16 11:36:18.812240','2026-11-16 12:07:03.951238',NULL,'2026-11-16 11:37:03.951238',0),
(286,X'150984e5e0a25fba5a5e2aaa330e45b318b0fb90f9b801db41e3afe8b9a8afb1','2026-11-16 11:46:20.993434','2026-11-16 18:08:43.227132',NULL,'2026-11-16 17:38:43.227132',0),
(287,X'b0af9b9efba3f9c217d87961c1a920e6d7967773d619080277256a65afef34f9','2026-11-16 12:05:33.690680','2026-11-16 12:35:34.190839','2026-11-16 12:05:34.426295','2026-11-16 12:05:34.190839',0),
(288,X'ded74c32704a19d5cf75a2f7da25b16dd54c09f8a6c66967993aff6497d056d5','2026-11-16 12:25:50.011738','2026-11-16 13:00:52.993003','2026-11-16 12:30:53.987480','2026-11-16 12:30:52.993003',0),
(289,X'8f3ddf2b93cab7643db91be15cbe12ca4bd6b4f245492b9317ec9a90428b88a2','2026-11-16 12:30:55.273618','2026-11-16 13:01:47.663215','2026-11-16 12:31:47.712797','2026-11-16 12:31:47.663215',0),
(290,X'3c8192fda806d9714ebef7a942094208396f77d2a156414263ffae2731861ae9','2026-11-16 12:31:48.909841','2026-11-16 13:52:17.069087','2026-11-16 13:22:18.729850','2026-11-16 13:22:17.069087',0),
(291,X'76573ed6a8d2728137411fb9078d75894c4e86b50ca8fc624eb589d7fa9edef7','2026-11-16 13:22:22.642238','2026-11-16 13:53:00.235874',NULL,'2026-11-16 13:23:00.235874',0),
(292,X'da34b06f4b407836646bfcbf689a892526d0c88f28be5a3146dfdf40090ec0f1','2026-11-16 13:54:14.306462','2026-11-16 14:42:20.063732',NULL,'2026-11-16 14:12:20.063732',0),
(293,X'2fac85701b6f0b7a58266ba3e5decb1d5f507f5e3adeaf559519ad786ccd4492','2026-11-16 14:53:05.386370','2026-11-16 15:23:05.640304','2026-11-16 14:53:06.202944','2026-11-16 14:53:05.640304',0),
(294,X'08e6349eb9c8371484aed4deb43a9c1f6748c1fcba4bf533e623501849cc4279','2026-11-16 14:53:07.518677','2026-11-16 16:00:19.067118',NULL,'2026-11-16 15:30:19.067118',0),
(295,X'e598529122509a4ea33b98612e85ebb5def62718ff050f8ff050b9106bb0a421','2026-11-16 17:19:07.835342','2026-11-16 19:49:33.693727','2026-11-16 19:19:43.717834','2026-11-16 19:19:33.693727',0),
(296,X'2b41f37fa7659bfdfad3984e0212ab90a2b31cc35e64a36979dd655f4c212b26','2026-11-16 18:11:18.506445','2026-11-16 21:43:36.625785',NULL,'2026-11-16 21:13:36.625785',0),
(297,X'47c4a5de08be1a0f9ef66ef8203227f10d2fefe901c708cb5ba127dbf9f8111d','2026-11-16 18:11:18.507086','2026-11-16 21:43:36.620573',NULL,'2026-11-16 21:13:36.620573',0),
(298,X'c268ea9ff469d1bc64c249807c7a93294e2fea756d17f81834216667a7ceaccb','2026-11-16 18:49:55.507450','2026-11-16 19:20:02.169223',NULL,'2026-11-16 18:50:02.169223',0),
(299,X'ca85ece02983c22c50cd877f97cd31a9c960b85dfa2e4df9d37b64b6ecc7710c','2026-11-16 18:49:59.526445','2026-11-16 19:45:12.255272',NULL,'2026-11-16 19:15:12.255272',0),
(300,X'91f28028c3704a6fcac2c569e96bfd84dd9e3abb598681863ca378200981574a','2026-11-16 21:44:59.616697','2026-11-16 23:16:33.598921',NULL,'2026-11-16 22:46:33.598921',0),
(301,X'ccb8dd9621d94f73fe40700bbc751e7c40e65cb612045b1d5f482d9ecbc79816','2026-11-16 21:44:59.625789','2026-11-16 23:16:33.601262',NULL,'2026-11-16 22:46:33.601262',0),
(302,X'a206d1b4029c4878495672c1c130ad690f8b2db545d620021cf7dbc8101d7cd0','2026-11-16 23:20:05.750250','2026-11-17 00:53:32.571925',NULL,'2026-11-17 00:23:32.571925',0),
(303,X'cbe6cdff3b1f7c34737f21e9a46e8346be398fcf614bdd795d46cb819bacc064','2026-11-16 23:20:05.777246','2026-11-17 00:53:32.562008',NULL,'2026-11-17 00:23:32.562008',0),
(304,X'15aa28df77b3dffe0b173598f1244c0d5fca17572dce26c6a13b816ef8572031','2026-11-17 01:12:54.738639','2026-11-17 01:42:55.105324',NULL,'2026-11-17 01:12:55.105324',0),
(305,X'ff4c3e38bfdf787eec39b0bf0a7dca840d8ac517c5f13ebec4c08a37ce18011a','2026-11-17 01:12:54.741206','2026-11-17 01:42:55.107668','2026-11-17 01:13:05.529416','2026-11-17 01:12:55.107668',0),
(306,X'45ff0f31edf6d724848ddd9be2c59fd935f6ebb468e5629478f45f394632c4ed','2026-11-17 02:30:46.560421','2026-11-17 03:08:21.270168','2026-11-17 02:38:22.043165','2026-11-17 02:38:21.270168',0),
(307,X'ee4ce025c52b7ed66c744a6a4e1a5837bd220f2c77815dee487beb643a2c9d37','2026-11-17 02:38:24.263963','2026-11-17 03:12:15.195465',NULL,'2026-11-17 02:42:15.195465',0),
(308,X'b2571136802b4cc8ecb4dac25cfbb5fd6eabf98b80dd13d5221e900b02d1f47f','2026-11-17 03:17:52.523075','2026-11-17 04:26:52.949829',NULL,'2026-11-17 03:56:52.949829',0),
(309,X'10894cc8375ac7eb4c00f229f0d9b78a8b8ae2c42c7525cea1ffd6ac62db67d1','2026-11-17 03:35:38.076531','2026-11-17 04:05:52.040071','2026-11-17 03:35:54.998082','2026-11-17 03:35:52.040071',0),
(310,X'3263d9c8cca315860f4bfa22cab337576e5a498ecaa7e6d479875f24dfba082b','2026-11-17 03:36:05.326665','2026-11-17 04:06:08.329874','2026-11-17 03:36:09.117495','2026-11-17 03:36:08.329874',0),
(311,X'b2834d8ba742b7f1ed7306fa6b3afa40aa6b55b1bdae38585b96c89d7b200a30','2026-11-17 03:36:10.235136','2026-11-17 04:06:25.096531','2026-11-17 03:36:27.869856','2026-11-17 03:36:25.096531',0),
(312,X'd0f781adf3c2c8660a8ddff77b263558e793c29acf1138f2d7a6903ef0b6ca15','2026-11-17 03:36:29.091050','2026-11-17 04:06:29.493167',NULL,'2026-11-17 03:36:29.493167',0),
(313,X'3a1fea0010fb0fd5b7087de640598884af029c5c59ee74bdfcb076c9da21f54e','2026-11-17 03:36:59.382665','2026-11-17 04:07:49.237265',NULL,'2026-11-17 03:37:49.237265',0),
(314,X'cc68bdb88c04331d8968c43e1134dfefb7a6e500f579f97308847ad0e1c8cdb4','2026-11-17 03:37:17.242449','2026-11-17 04:26:33.055651',NULL,'2026-11-17 03:56:33.055651',0),
(315,X'3c0a1d79f0355ad4a9af4b46b9f2306e69c7ac2ed210c7ddc08e870b9a2b62ff','2026-11-17 03:55:55.532267','2026-11-17 04:25:55.843129','2026-11-17 03:55:59.623222','2026-11-17 03:55:55.843129',0),
(316,X'24a7b19d64a986f2b7d4c5727a7f0c17da3a3695f7a394fea1be7e622170599a','2026-11-17 03:56:00.975498','2026-11-17 04:26:36.776486',NULL,'2026-11-17 03:56:36.776486',0),
(317,X'd1bdd89ca60afc4c115e721e5c6dc5f79f370dabf25b249f07f1d8c266800247','2026-11-17 04:03:54.433190','2026-11-17 04:34:43.526720',NULL,'2026-11-17 04:04:43.526720',0),
(318,X'd1f2eb8b6002ef7f9e66be1123d4feadf0c508380bd5b1d422a784eaf1938b8a','2026-11-17 04:50:42.238955','2026-11-17 06:05:07.004502',NULL,'2026-11-17 05:35:07.004502',0),
(319,X'9e8b7cebfeb57742617de0592c24b88fe077883dfa1390680e65355d9d72275a','2026-11-17 05:00:12.377627','2026-11-17 05:52:07.710395','2026-11-17 05:22:08.688767','2026-11-17 05:22:07.710395',0),
(320,X'08a934bfa6777f09c943aa6851a254a7681db9d6819e72a0f2215398da234634','2026-11-17 05:22:11.658630','2026-11-17 05:57:25.193852','2026-11-17 05:27:25.299187','2026-11-17 05:27:25.193852',0),
(321,X'ecf7541b31c29a807abc0e05bfa6cb873368cc955d485c788bc320762b025341','2026-11-17 05:27:26.558172','2026-11-17 06:05:51.646316',NULL,'2026-11-17 05:35:51.646316',0),
(322,X'b15d25fcbfc9e433fbc23ebc32a404f0a3f4bd9c4ce8134e80e09020b1de42ed','2026-11-17 06:54:51.969251','2026-11-17 07:24:52.697163','2026-11-17 06:54:53.831127','2026-11-17 06:54:52.697163',0),
(323,X'1ae4898bcf1a1e4c7803f9a732f8cb681b6245ee11044c4da69a62d992810edb','2026-11-17 06:55:05.245585','2026-11-17 07:33:20.011977',NULL,'2026-11-17 07:03:20.011977',0),
(324,X'1d63238a72a5d1f315a7471a4db237c8faac260306131ae519436f9df35f290e','2026-11-17 07:49:15.755245','2026-11-17 08:19:39.744571',NULL,'2026-11-17 07:49:39.744571',0),
(325,X'12121bace3237c6469d5dac78e8ca31c5b34fad8b201882528f4d0c0f54b9013','2026-11-17 07:49:18.403010','2026-11-17 08:35:19.765472',NULL,'2026-11-17 08:05:19.765472',0),
(326,X'f0d8148343377b1f4f31f8dd2e3b9d8c1c1bd7c11fcfbaddba4e4ee198b30111','2026-11-17 08:52:00.136559','2026-11-17 09:22:00.657657',NULL,'2026-11-17 08:52:00.657657',0),
(327,X'6ebebaed28dc0e54005d9b83633c6d5c9ed849bb795780f9b50728f492713128','2026-11-17 11:52:56.480219','2026-11-17 12:22:56.749001','2026-11-17 11:52:58.979478','2026-11-17 11:52:56.749001',0),
(328,X'6e0a4242cb4d2c765656bfb8ee7ce6179207088315e1a268e8611f7a59482296','2026-11-17 16:59:09.847059','2026-11-17 17:35:19.595706',NULL,'2026-11-17 17:05:19.595706',0),
(329,X'2969b3d44d7426409a21fd8d5d7f43dfacd165ab797d1866ef8481871d402405','2026-11-17 16:59:17.119899','2026-11-17 17:35:19.594438',NULL,'2026-11-17 17:05:19.594438',0),
(330,X'1bae59214e101716e072bf0758efcc70077430e7b3e8f28649ac807741cfb28d','2026-11-17 19:38:58.146445','2026-11-17 20:09:01.472873','2026-11-17 19:39:03.032870','2026-11-17 19:39:01.472873',0),
(331,X'9dacf7cbf302f4099d2af9fd215826a50a51d51c2e16b07d792bb83006d98446','2026-11-18 07:08:17.394088','2026-11-18 07:38:27.988444','2026-11-18 07:08:28.728394','2026-11-18 07:08:27.988444',0),
(332,X'187a55a2a84ce9372010fcde28a405ddf64f72cb6925283b243b7d6ba9d0ba8d','2026-11-18 12:17:50.028031','2026-11-18 12:47:50.028031',NULL,'2026-11-18 12:17:50.028031',0),
(333,X'6bf606e413e067b998a31cdd22d80d895af8ae8a085c51f899a27a3c86703f18','2026-11-18 12:17:51.200463','2026-11-18 12:47:51.200463',NULL,'2026-11-18 12:17:51.200463',0),
(334,X'4437d75ef9ddcbc9d7556aad3fc0a36b40738589017a81404fc0cbc0397124aa','2026-11-18 12:17:53.277457','2026-11-18 12:50:27.945085','2026-11-18 12:20:30.152652','2026-11-18 12:20:27.945085',0),
(335,X'eeed70eec553eac71362b9610c0ea61843e358679c98630ae24a3ecdbc5c26a2','2026-11-18 12:19:10.722136','2026-11-18 12:49:17.288732',NULL,'2026-11-18 12:19:17.288732',0),
(336,X'60e06bddc38b0fd0a7c26a5d7cae3ad34a524cf186a8462fc1d9191525dec1b9','2026-11-18 19:53:07.865430','2026-11-18 20:23:26.616521','2026-11-18 19:53:28.364048','2026-11-18 19:53:26.616521',0),
(337,X'39c570d63f02677be10341366bcb3abdf6aefc34b162569dfd83aceb73f6f421','2026-11-19 18:08:59.372553','2026-11-19 18:39:00.266958','2026-11-19 18:09:00.990135','2026-11-19 18:09:00.266958',0),
(338,X'c4f5f4dc9a4f0b164385af8cccf1ff1f87484ef36679ec6a999067fbc62be783','2026-11-20 04:56:48.097764','2026-11-20 05:27:57.107457','2026-11-20 04:57:59.755714','2026-11-20 04:57:57.107457',0),
(339,X'37eff02021bec5ecbc83d7cd2a9043d7438b9bcf135855c73676f5ddaa7840ac','2026-11-20 05:24:06.929679','2026-11-20 05:54:17.219272','2026-11-20 05:24:17.767266','2026-11-20 05:24:17.219272',0),
(340,X'5e42993e42d7b9ae27345526a19675c6519a658b8c77404847db5b257930e399','2026-11-20 05:39:27.639660','2026-11-20 06:09:28.962090','2026-11-20 05:39:30.394041','2026-11-20 05:39:28.962090',0),
(341,X'faf8e6f450c9d9ab8a67db12447e2c3d2b333be798ad954ae863ed807c8b703e','2026-11-20 05:42:31.801451','2026-11-20 07:00:44.844535','2026-11-20 06:30:45.833338','2026-11-20 06:30:44.844535',0),
(342,X'4100ff3e79766da0bdc81544174c3c193e9ad81db7953cbfb94b686e9ee31808','2026-11-20 05:53:19.849443','2026-11-20 06:35:07.089201','2026-11-20 06:05:07.998353','2026-11-20 06:05:07.089201',0),
(343,X'16520582376250c3c578816e4f6624945acc0e181a5fd47cac64a0dcdc51bffc','2026-11-20 06:42:18.367330','2026-11-20 07:14:14.592323','2026-11-20 06:44:17.315364','2026-11-20 06:44:14.592323',0),
(344,X'f48a78a3f5ae39a8639f5d1332a90b3d5668d371628bab2007ff74782d748f02','2026-11-20 07:11:30.991446','2026-11-20 10:26:23.192658',NULL,'2026-11-20 09:56:23.192658',0),
(345,X'38c09edb8881c6e078764e7293bb1ae1629b1299fff36a3e6568552a2c78e86f','2026-11-20 07:51:21.001569','2026-11-20 08:21:23.994531',NULL,'2026-11-20 07:51:23.994531',0),
(346,X'806920e1c1f3f71863477427374b90d24b522ac2455b059acfc755b0dc88c45a','2026-11-20 07:52:24.451453','2026-11-20 08:29:13.138809','2026-11-20 07:59:14.746624','2026-11-20 07:59:13.138809',0),
(347,X'0f6285cc71f8eb00b58838c1a74ba2d3e1e3c693ee7f964dd344d5b2bd17baa4','2026-11-20 07:58:44.137661','2026-11-20 08:28:44.788002','2026-11-20 07:58:47.106670','2026-11-20 07:58:44.788002',0),
(348,X'efc09a018412cc1ade83fab311df65019887a6d512850372933dd5c6d3dc1897','2026-11-20 09:09:19.272431','2026-11-20 09:47:31.956048',NULL,'2026-11-20 09:17:31.956048',0),
(349,X'9a30eb94c27d3238190be8bfad88e3bacf37003ea160562d1e2e7f2d576322e8','2026-11-20 09:16:57.749115','2026-11-20 09:47:00.746591','2026-11-20 09:17:02.546163','2026-11-20 09:17:00.746591',0),
(350,X'91255e3adb1da03c13dbb44392edf30f3fa2a2383991b7788e367e46617444f7','2026-11-20 09:27:36.173562','2026-11-20 09:58:02.350506','2026-11-20 09:28:03.460605','2026-11-20 09:28:02.350506',0),
(351,X'6801dbe34831e2000a4264b6974b888f1766c2b50a49b51998c6a6e9fc2b9f87','2026-11-20 09:28:04.962465','2026-11-20 09:59:57.756826','2026-11-20 09:29:57.870949','2026-11-20 09:29:57.756826',0),
(352,X'b4831a6131b7e780211ea6da9ad66c88f63f183071a6411047de2b7400121245','2026-11-20 09:53:40.631875','2026-11-20 10:23:52.526446','2026-11-20 09:53:53.932641','2026-11-20 09:53:52.526446',0),
(353,X'41fcc0ad2c2ab07d1af066aeef39bfe6a3a468de014a075bb7017ad71973abd5','2026-11-20 10:32:20.024582','2026-11-20 11:02:20.346836',NULL,'2026-11-20 10:32:20.346836',0),
(354,X'85a6ea968aed57a7c26f2a1ba42f312df032bd463d2f670b0ddbb264fc386c07','2026-11-20 11:56:53.166921','2026-11-20 12:58:26.924690','2026-11-20 12:28:27.208730','2026-11-20 12:28:26.924690',0),
(355,X'88068b4ae512415a20a03622cf3fbe07f32b310943670aea151cd80d5282e676','2026-11-20 18:16:49.458770','2026-11-20 18:47:11.113844','2026-11-20 18:17:12.148271','2026-11-20 18:17:11.113844',0),
(356,X'ecd7fbc85c31681b3eeaf29c6b686f89a456471174a808a0ccafb500cfd2a853','2026-11-20 19:08:25.265997','2026-11-20 19:38:30.465069','2026-11-20 19:08:31.449465','2026-11-20 19:08:30.465069',0),
(357,X'6215a1be22c1fc80c9a00e0a3ea0a62bcd760e146c8ce6a71dba4098d0c80944','2026-11-20 19:53:10.955452','2026-11-20 20:23:27.938867','2026-11-20 19:53:30.691857','2026-11-20 19:53:27.938867',0),
(358,X'e79adaf938f2e81f0d104e36c5e0903275b215f2a1adf2e84d1b4290262f1dc2','2026-11-21 07:02:04.321955','2026-11-21 07:32:13.521921',NULL,'2026-11-21 07:02:13.521921',0),
(359,X'd22865038ed12b47e38306e409062fa910680f972561dc7d53b1ec42bc3d8507','2026-11-22 08:08:19.617254','2026-11-22 08:40:34.622291','2026-11-22 08:10:37.008794','2026-11-22 08:10:34.622291',0),
(360,X'ad978e34ce2d80d5787720fe2f5367363a54e40aafd60237a89e0f3cadbc83eb','2026-11-24 10:04:41.184634','2026-11-24 10:34:41.776452',NULL,'2026-11-24 10:04:41.776452',0),
(361,X'07958ea85b9c690071ba64e0a0aa3153f2a0d61d31a661829037abac2416ee64','2026-11-24 14:43:50.651157','2026-11-24 15:14:35.636873','2026-11-24 14:44:36.614483','2026-11-24 14:44:35.636873',0),
(362,X'5b41134c689027ccf4641b8d306c2720268fead6c2b2fa60efe31093af658827','2026-11-24 14:44:38.193675','2026-11-24 16:33:45.179306',NULL,'2026-11-24 16:03:45.179306',0),
(363,X'f015961a6c0edaedec8e54cc2d3c5bf84c84b44bf52abb62e11312d6bbb94fae','2026-11-24 14:50:30.203446','2026-11-24 15:20:33.210356','2026-11-24 14:50:34.302193','2026-11-24 14:50:33.210356',0),
(364,X'1f469ba571f48d1e56df1100c4a8039d51ee1b76d3c1e5ca8c1cce4d3fd5009d','2026-11-24 14:50:35.279198','2026-11-24 15:22:35.037946','2026-11-24 14:52:36.228612','2026-11-24 14:52:35.037946',0),
(365,X'6d61cbb5959389802e6be4e93750ad9a7aa3cf047b75f4f3c1e886df42fbe179','2026-11-24 14:52:37.192718','2026-11-24 15:22:43.359015','2026-11-24 14:52:44.906450','2026-11-24 14:52:43.359015',0),
(366,X'fc70f594daa77a248cb44a2e8b663841cb67fbf762426ad68ddca628e40e80c2','2026-11-24 15:24:21.332449','2026-11-24 16:10:49.631307',NULL,'2026-11-24 15:40:49.631307',0);
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

-- Dump completed on 2026-11-25  0:02:51
