-- OPTIONAL demo catalog only. Run manually after importing the user's original schema.
-- No DROP/TRUNCATE/DELETE, no existing passwords or data are changed.
USE SGU_audio_guide;
SET NAMES utf8mb4;
INSERT INTO languages(language_code,language_name,is_active)
SELECT 'vi','Tiếng Việt',1 WHERE NOT EXISTS(SELECT 1 FROM languages WHERE language_code='vi');
INSERT INTO languages(language_code,language_name,is_active)
SELECT 'en','English',1 WHERE NOT EXISTS(SELECT 1 FROM languages WHERE language_code='en');
INSERT INTO campus_areas(area_code,area_name,description,is_visible)
SELECT 'SGU-DEMO','Khuôn viên SGU - minh họa','Khu vực mẫu; cập nhật theo khảo sát của nhóm.',1
WHERE NOT EXISTS(SELECT 1 FROM campus_areas WHERE area_code='SGU-DEMO');
