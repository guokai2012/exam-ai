-- 清理 BaseEntity 改造后的历史审计字段残留。
-- 旧字段数据先回填到 create_id/create_time/update_id/update_time，再按存在性判断删除旧列，支持重复执行。

DROP PROCEDURE IF EXISTS run_sql_if_column_exists_legacy_audit;
DROP PROCEDURE IF EXISTS drop_index_if_exists_legacy_audit;
DROP PROCEDURE IF EXISTS drop_column_if_exists_legacy_audit;
DROP PROCEDURE IF EXISTS add_index_if_missing_legacy_audit;

DELIMITER $$
CREATE PROCEDURE run_sql_if_column_exists_legacy_audit(
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

CREATE PROCEDURE drop_index_if_exists_legacy_audit(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64)
)
BEGIN
    IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND INDEX_NAME = index_name_param) > 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_param, '`', '``'),
            '` DROP INDEX `', REPLACE(index_name_param, '`', '``'), '`'
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE drop_column_if_exists_legacy_audit(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64)
)
BEGIN
    IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND COLUMN_NAME = column_name_param) > 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_param, '`', '``'),
            '` DROP COLUMN `', REPLACE(column_name_param, '`', '``'), '`'
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_index_if_missing_legacy_audit(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64),
    IN ddl_param TEXT
)
BEGIN
    IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND INDEX_NAME = index_name_param) = 0 THEN
        SET @sql = ddl_param;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL run_sql_if_column_exists_legacy_audit('exam_document', 'uploaded_by',
    'UPDATE exam_document SET create_id = uploaded_by WHERE create_id = 0 AND uploaded_by IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document_analysis', 'created_by',
    'UPDATE exam_document_analysis SET create_id = created_by WHERE create_id = 0 AND created_by IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_category', 'created_by',
    'UPDATE exam_question_category SET create_id = created_by WHERE create_id = 0 AND created_by IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_bank', 'created_by',
    'UPDATE exam_question_bank SET create_id = created_by WHERE create_id = 0 AND created_by IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_config', 'updated_by',
    'UPDATE sys_config SET update_id = updated_by WHERE update_id = 0 AND updated_by IS NOT NULL');

CALL run_sql_if_column_exists_legacy_audit('sys_user', 'created_at',
    'UPDATE sys_user SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_user', 'updated_at',
    'UPDATE sys_user SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_role', 'created_at',
    'UPDATE sys_role SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_permission', 'created_at',
    'UPDATE sys_permission SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_refresh_token', 'created_at',
    'UPDATE sys_refresh_token SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_menu', 'created_at',
    'UPDATE sys_menu SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_menu', 'updated_at',
    'UPDATE sys_menu SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document', 'created_at',
    'UPDATE exam_document SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document', 'updated_at',
    'UPDATE exam_document SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document_analysis', 'created_at',
    'UPDATE exam_document_analysis SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document_analysis', 'updated_at',
    'UPDATE exam_document_analysis SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document_analysis_chunk', 'created_at',
    'UPDATE exam_document_analysis_chunk SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_document_analysis_chunk', 'updated_at',
    'UPDATE exam_document_analysis_chunk SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_category', 'created_at',
    'UPDATE exam_question_category SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_category', 'updated_at',
    'UPDATE exam_question_category SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_bank', 'created_at',
    'UPDATE exam_question_bank SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_bank', 'updated_at',
    'UPDATE exam_question_bank SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_tag', 'created_at',
    'UPDATE exam_question_tag SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_tag', 'updated_at',
    'UPDATE exam_question_tag SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_tag_relation', 'created_at',
    'UPDATE exam_question_tag_relation SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('exam_question_source', 'created_at',
    'UPDATE exam_question_source SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_config', 'created_at',
    'UPDATE sys_config SET create_time = created_at WHERE created_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_config', 'updated_at',
    'UPDATE sys_config SET update_time = updated_at WHERE updated_at IS NOT NULL');
CALL run_sql_if_column_exists_legacy_audit('sys_notification', 'created_at',
    'UPDATE sys_notification SET create_time = created_at WHERE created_at IS NOT NULL');

CALL drop_index_if_exists_legacy_audit('exam_document', 'idx_document_uploaded_by');
CALL drop_index_if_exists_legacy_audit('exam_document_analysis', 'idx_analysis_created_by');
CALL drop_index_if_exists_legacy_audit('exam_question_category', 'fk_question_category_created_by');
CALL drop_index_if_exists_legacy_audit('sys_config', 'fk_sys_config_updated_by');
CALL drop_index_if_exists_legacy_audit('sys_notification', 'idx_notification_recipient');

CALL drop_column_if_exists_legacy_audit('exam_document', 'uploaded_by');

CALL drop_column_if_exists_legacy_audit('sys_user', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_user', 'updated_at');
CALL drop_column_if_exists_legacy_audit('sys_role', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_permission', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_refresh_token', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_menu', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_menu', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_document', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_document', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_document_analysis', 'created_by');
CALL drop_column_if_exists_legacy_audit('exam_document_analysis', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_document_analysis', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_document_analysis_chunk', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_document_analysis_chunk', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_question_category', 'created_by');
CALL drop_column_if_exists_legacy_audit('exam_question_category', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_question_category', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_question_bank', 'created_by');
CALL drop_column_if_exists_legacy_audit('exam_question_bank', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_question_bank', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_question_tag', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_question_tag', 'updated_at');
CALL drop_column_if_exists_legacy_audit('exam_question_tag_relation', 'created_at');
CALL drop_column_if_exists_legacy_audit('exam_question_source', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_config', 'updated_by');
CALL drop_column_if_exists_legacy_audit('sys_config', 'created_at');
CALL drop_column_if_exists_legacy_audit('sys_config', 'updated_at');
CALL drop_column_if_exists_legacy_audit('sys_notification', 'created_at');

CALL add_index_if_missing_legacy_audit('exam_document', 'idx_document_create_id',
    'ALTER TABLE exam_document ADD INDEX idx_document_create_id (create_id)');
CALL add_index_if_missing_legacy_audit('exam_document_analysis', 'idx_analysis_create_id',
    'ALTER TABLE exam_document_analysis ADD INDEX idx_analysis_create_id (create_id)');
CALL add_index_if_missing_legacy_audit('sys_notification', 'idx_notification_recipient',
    'ALTER TABLE sys_notification ADD INDEX idx_notification_recipient (recipient_id, read_at, create_time)');

DROP PROCEDURE IF EXISTS run_sql_if_column_exists_legacy_audit;
DROP PROCEDURE IF EXISTS drop_index_if_exists_legacy_audit;
DROP PROCEDURE IF EXISTS drop_column_if_exists_legacy_audit;
DROP PROCEDURE IF EXISTS add_index_if_missing_legacy_audit;
