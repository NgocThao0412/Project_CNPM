-- Run once only on databases without these columns.
-- Already applied on Thao's database.
USE SGU_audio_guide;

ALTER TABLE visitor_sessions
    ADD COLUMN last_seen_at DATETIME(6) NULL
        COMMENT 'UTC timestamp of the latest heartbeat',
    ADD COLUMN is_listening TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '1: playing audio; 0: idle, paused or ended',
    ADD INDEX ix_visit_last_seen (last_seen_at);
