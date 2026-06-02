-- 统一 BaseEntity 公共字段与逻辑删除字段；所有语句均带存在性判断，支持重复执行。

DROP PROCEDURE IF EXISTS drop_foreign_keys_touching;

DELIMITER $$
CREATE PROCEDURE drop_foreign_keys_touching(IN target_table_param VARCHAR(64))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE fk_table_name VARCHAR(64);
    DECLARE fk_constraint_name VARCHAR(64);
    DECLARE fk_cursor CURSOR FOR
        SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME
        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
        WHERE kcu.TABLE_SCHEMA = DATABASE()
          AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
          AND (
              kcu.TABLE_NAME = target_table_param
              OR kcu.REFERENCED_TABLE_NAME = target_table_param
          );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN fk_cursor;
    drop_loop: LOOP
        FETCH fk_cursor INTO fk_table_name, fk_constraint_name;
        IF done THEN
            LEAVE drop_loop;
        END IF;
        SET @sql = CONCAT(
            'ALTER TABLE `', REPLACE(fk_table_name, '`', '``'),
            '` DROP FOREIGN KEY `', REPLACE(fk_constraint_name, '`', '``'), '`'
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE fk_cursor;
END$$
DELIMITER ;

CALL drop_foreign_keys_touching('sys_user');
CALL drop_foreign_keys_touching('sys_role');
CALL drop_foreign_keys_touching('sys_permission');
CALL drop_foreign_keys_touching('sys_user_role');
CALL drop_foreign_keys_touching('sys_role_permission');
CALL drop_foreign_keys_touching('sys_refresh_token');
CALL drop_foreign_keys_touching('sys_menu');
CALL drop_foreign_keys_touching('exam_document');
CALL drop_foreign_keys_touching('exam_document_analysis');
CALL drop_foreign_keys_touching('exam_document_analysis_chunk');
CALL drop_foreign_keys_touching('exam_question_category');
CALL drop_foreign_keys_touching('exam_question_bank');
CALL drop_foreign_keys_touching('exam_question_tag');
CALL drop_foreign_keys_touching('exam_question_tag_relation');
CALL drop_foreign_keys_touching('exam_question_source');
CALL drop_foreign_keys_touching('sys_config');
CALL drop_foreign_keys_touching('sys_notification');

DROP PROCEDURE IF EXISTS drop_foreign_keys_touching;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sys_user_role DROP PRIMARY KEY',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sys_user_role ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role_permission' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sys_role_permission DROP PRIMARY KEY',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role_permission' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sys_role_permission ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_question_tag_relation' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE exam_question_tag_relation DROP PRIMARY KEY',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_question_tag_relation' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE exam_question_tag_relation ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sys_config DROP PRIMARY KEY',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sys_config ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64),
    IN ddl_param TEXT
)
BEGIN
    IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND COLUMN_NAME = column_name_param) = 0 THEN
        SET @sql = ddl_param;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('sys_user', 'create_id', 'ALTER TABLE sys_user ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_user', 'create_time', 'ALTER TABLE sys_user ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_user', 'update_id', 'ALTER TABLE sys_user ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_user', 'update_time', 'ALTER TABLE sys_user ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_user', 'deleted', 'ALTER TABLE sys_user ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_role', 'create_id', 'ALTER TABLE sys_role ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_role', 'create_time', 'ALTER TABLE sys_role ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_role', 'update_id', 'ALTER TABLE sys_role ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_role', 'update_time', 'ALTER TABLE sys_role ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_role', 'deleted', 'ALTER TABLE sys_role ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_permission', 'create_id', 'ALTER TABLE sys_permission ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_permission', 'create_time', 'ALTER TABLE sys_permission ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_permission', 'update_id', 'ALTER TABLE sys_permission ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_permission', 'update_time', 'ALTER TABLE sys_permission ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_permission', 'deleted', 'ALTER TABLE sys_permission ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_user_role', 'create_id', 'ALTER TABLE sys_user_role ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_user_role', 'create_time', 'ALTER TABLE sys_user_role ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_user_role', 'update_id', 'ALTER TABLE sys_user_role ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_user_role', 'update_time', 'ALTER TABLE sys_user_role ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_user_role', 'deleted', 'ALTER TABLE sys_user_role ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_role_permission', 'create_id', 'ALTER TABLE sys_role_permission ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_role_permission', 'create_time', 'ALTER TABLE sys_role_permission ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_role_permission', 'update_id', 'ALTER TABLE sys_role_permission ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_role_permission', 'update_time', 'ALTER TABLE sys_role_permission ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_role_permission', 'deleted', 'ALTER TABLE sys_role_permission ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_refresh_token', 'create_id', 'ALTER TABLE sys_refresh_token ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_refresh_token', 'create_time', 'ALTER TABLE sys_refresh_token ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_refresh_token', 'update_id', 'ALTER TABLE sys_refresh_token ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_refresh_token', 'update_time', 'ALTER TABLE sys_refresh_token ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_refresh_token', 'deleted', 'ALTER TABLE sys_refresh_token ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_menu', 'create_id', 'ALTER TABLE sys_menu ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_menu', 'create_time', 'ALTER TABLE sys_menu ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_menu', 'update_id', 'ALTER TABLE sys_menu ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_menu', 'update_time', 'ALTER TABLE sys_menu ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_menu', 'deleted', 'ALTER TABLE sys_menu ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_document', 'create_id', 'ALTER TABLE exam_document ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_document', 'create_time', 'ALTER TABLE exam_document ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_document', 'update_id', 'ALTER TABLE exam_document ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_document', 'update_time', 'ALTER TABLE exam_document ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_document', 'deleted', 'ALTER TABLE exam_document ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_document_analysis', 'create_id', 'ALTER TABLE exam_document_analysis ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_document_analysis', 'create_time', 'ALTER TABLE exam_document_analysis ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_document_analysis', 'update_id', 'ALTER TABLE exam_document_analysis ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_document_analysis', 'update_time', 'ALTER TABLE exam_document_analysis ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_document_analysis', 'deleted', 'ALTER TABLE exam_document_analysis ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_document_analysis_chunk', 'create_id', 'ALTER TABLE exam_document_analysis_chunk ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_document_analysis_chunk', 'create_time', 'ALTER TABLE exam_document_analysis_chunk ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_document_analysis_chunk', 'update_id', 'ALTER TABLE exam_document_analysis_chunk ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_document_analysis_chunk', 'update_time', 'ALTER TABLE exam_document_analysis_chunk ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_document_analysis_chunk', 'deleted', 'ALTER TABLE exam_document_analysis_chunk ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_question_category', 'create_id', 'ALTER TABLE exam_question_category ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_question_category', 'create_time', 'ALTER TABLE exam_question_category ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_question_category', 'update_id', 'ALTER TABLE exam_question_category ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_question_category', 'update_time', 'ALTER TABLE exam_question_category ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_question_category', 'deleted', 'ALTER TABLE exam_question_category ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_question_bank', 'create_id', 'ALTER TABLE exam_question_bank ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_question_bank', 'create_time', 'ALTER TABLE exam_question_bank ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_question_bank', 'update_id', 'ALTER TABLE exam_question_bank ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_question_bank', 'update_time', 'ALTER TABLE exam_question_bank ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_question_bank', 'deleted', 'ALTER TABLE exam_question_bank ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_question_tag', 'create_id', 'ALTER TABLE exam_question_tag ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_question_tag', 'create_time', 'ALTER TABLE exam_question_tag ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_question_tag', 'update_id', 'ALTER TABLE exam_question_tag ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_question_tag', 'update_time', 'ALTER TABLE exam_question_tag ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_question_tag', 'deleted', 'ALTER TABLE exam_question_tag ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_question_tag_relation', 'create_id', 'ALTER TABLE exam_question_tag_relation ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_question_tag_relation', 'create_time', 'ALTER TABLE exam_question_tag_relation ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_question_tag_relation', 'update_id', 'ALTER TABLE exam_question_tag_relation ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_question_tag_relation', 'update_time', 'ALTER TABLE exam_question_tag_relation ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_question_tag_relation', 'deleted', 'ALTER TABLE exam_question_tag_relation ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('exam_question_source', 'create_id', 'ALTER TABLE exam_question_source ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('exam_question_source', 'create_time', 'ALTER TABLE exam_question_source ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('exam_question_source', 'update_id', 'ALTER TABLE exam_question_source ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('exam_question_source', 'update_time', 'ALTER TABLE exam_question_source ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('exam_question_source', 'deleted', 'ALTER TABLE exam_question_source ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_config', 'create_id', 'ALTER TABLE sys_config ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_config', 'create_time', 'ALTER TABLE sys_config ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_config', 'update_id', 'ALTER TABLE sys_config ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_config', 'update_time', 'ALTER TABLE sys_config ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_config', 'deleted', 'ALTER TABLE sys_config ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing('sys_notification', 'create_id', 'ALTER TABLE sys_notification ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing('sys_notification', 'create_time', 'ALTER TABLE sys_notification ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing('sys_notification', 'update_id', 'ALTER TABLE sys_notification ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing('sys_notification', 'update_time', 'ALTER TABLE sys_notification ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing('sys_notification', 'deleted', 'ALTER TABLE sys_notification ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

DROP PROCEDURE IF EXISTS add_column_if_missing;

DROP PROCEDURE IF EXISTS run_sql_if_column_exists;

DELIMITER $$
CREATE PROCEDURE run_sql_if_column_exists(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64),
    IN sql_param TEXT
)
BEGIN
    IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND COLUMN_NAME = column_name_param) > 0 THEN
        SET @sql = sql_param;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL run_sql_if_column_exists('exam_document', 'uploaded_by', 'UPDATE exam_document SET create_id = uploaded_by WHERE create_id = 0 AND uploaded_by IS NOT NULL');
CALL run_sql_if_column_exists('exam_document_analysis', 'created_by', 'UPDATE exam_document_analysis SET create_id = created_by WHERE create_id = 0 AND created_by IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_category', 'created_by', 'UPDATE exam_question_category SET create_id = created_by WHERE create_id = 0 AND created_by IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_bank', 'created_by', 'UPDATE exam_question_bank SET create_id = created_by WHERE create_id = 0 AND created_by IS NOT NULL');
CALL run_sql_if_column_exists('sys_config', 'updated_by', 'UPDATE sys_config SET update_id = updated_by WHERE update_id = 0 AND updated_by IS NOT NULL');

CALL run_sql_if_column_exists('sys_user', 'created_at', 'UPDATE sys_user SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_user', 'updated_at', 'UPDATE sys_user SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_role', 'created_at', 'UPDATE sys_role SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_permission', 'created_at', 'UPDATE sys_permission SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_refresh_token', 'created_at', 'UPDATE sys_refresh_token SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_menu', 'created_at', 'UPDATE sys_menu SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_menu', 'updated_at', 'UPDATE sys_menu SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_document', 'created_at', 'UPDATE exam_document SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_document', 'updated_at', 'UPDATE exam_document SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_document_analysis', 'created_at', 'UPDATE exam_document_analysis SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_document_analysis', 'updated_at', 'UPDATE exam_document_analysis SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_document_analysis_chunk', 'created_at', 'UPDATE exam_document_analysis_chunk SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_document_analysis_chunk', 'updated_at', 'UPDATE exam_document_analysis_chunk SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_category', 'created_at', 'UPDATE exam_question_category SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_category', 'updated_at', 'UPDATE exam_question_category SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_bank', 'created_at', 'UPDATE exam_question_bank SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_bank', 'updated_at', 'UPDATE exam_question_bank SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_tag', 'created_at', 'UPDATE exam_question_tag SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_tag', 'updated_at', 'UPDATE exam_question_tag SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_tag_relation', 'created_at', 'UPDATE exam_question_tag_relation SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('exam_question_source', 'created_at', 'UPDATE exam_question_source SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_config', 'created_at', 'UPDATE sys_config SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_config', 'updated_at', 'UPDATE sys_config SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists('sys_notification', 'created_at', 'UPDATE sys_notification SET create_time = created_at WHERE created_at IS NOT NULL');

DROP PROCEDURE IF EXISTS run_sql_if_column_exists;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_sys_user_username') > 0, 'ALTER TABLE sys_user DROP INDEX uk_sys_user_username', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_username (username, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND INDEX_NAME = 'uk_sys_role_code') > 0, 'ALTER TABLE sys_role DROP INDEX uk_sys_role_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE sys_role ADD UNIQUE KEY uk_sys_role_code (role_code, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND INDEX_NAME = 'uk_sys_permission_code') > 0, 'ALTER TABLE sys_permission DROP INDEX uk_sys_permission_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE sys_permission ADD UNIQUE KEY uk_sys_permission_code (permission_code, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_refresh_token' AND INDEX_NAME = 'uk_refresh_token_hash') > 0, 'ALTER TABLE sys_refresh_token DROP INDEX uk_refresh_token_hash', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE sys_refresh_token ADD UNIQUE KEY uk_refresh_token_hash (token_hash, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_document_analysis_chunk' AND INDEX_NAME = 'uk_analysis_chunk_index') > 0, 'ALTER TABLE exam_document_analysis_chunk DROP INDEX uk_analysis_chunk_index', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE exam_document_analysis_chunk ADD UNIQUE KEY uk_analysis_chunk_index (analysis_id, chunk_index, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_question_category' AND INDEX_NAME = 'uk_question_category_name') > 0, 'ALTER TABLE exam_question_category DROP INDEX uk_question_category_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE exam_question_category ADD UNIQUE KEY uk_question_category_name (category_name, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_question_bank' AND INDEX_NAME = 'uk_question_owner_category_stem_hash') > 0, 'ALTER TABLE exam_question_bank DROP INDEX uk_question_owner_category_stem_hash', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE exam_question_bank ADD UNIQUE KEY uk_question_owner_category_stem_hash (create_id, category_id, stem_hash, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_question_tag' AND INDEX_NAME = 'uk_question_tag_name') > 0, 'ALTER TABLE exam_question_tag DROP INDEX uk_question_tag_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE exam_question_tag ADD UNIQUE KEY uk_question_tag_name (tag_name, deleted);

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config' AND INDEX_NAME = 'uk_sys_config_key') = 0, 'ALTER TABLE sys_config ADD UNIQUE KEY uk_sys_config_key (config_key, deleted)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_role' AND INDEX_NAME = 'uk_sys_user_role_pair') = 0, 'ALTER TABLE sys_user_role ADD UNIQUE KEY uk_sys_user_role_pair (user_id, role_id, deleted)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role_permission' AND INDEX_NAME = 'uk_sys_role_permission_pair') = 0, 'ALTER TABLE sys_role_permission ADD UNIQUE KEY uk_sys_role_permission_pair (role_id, permission_id, deleted)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_question_tag_relation' AND INDEX_NAME = 'uk_question_tag_relation_pair') = 0, 'ALTER TABLE exam_question_tag_relation ADD UNIQUE KEY uk_question_tag_relation_pair (question_id, tag_id, deleted)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
