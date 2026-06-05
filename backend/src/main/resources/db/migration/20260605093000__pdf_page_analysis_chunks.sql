-- 将文档分析分块调整为 PDF 页图片解析任务。
-- 本脚本保留 BaseEntity 公共字段，删除文本 chunk 字段，并以 page_no 作为页级解析单元。

DROP PROCEDURE IF EXISTS add_column_if_missing_pdf_page;
DROP PROCEDURE IF EXISTS drop_column_if_exists_pdf_page;
DROP PROCEDURE IF EXISTS drop_index_if_exists_pdf_page;
DROP PROCEDURE IF EXISTS add_index_if_missing_pdf_page;
DROP PROCEDURE IF EXISTS run_sql_if_column_exists_pdf_page;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_pdf_page(
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

CREATE PROCEDURE drop_column_if_exists_pdf_page(
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

CREATE PROCEDURE drop_index_if_exists_pdf_page(
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

CREATE PROCEDURE add_index_if_missing_pdf_page(
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

CREATE PROCEDURE run_sql_if_column_exists_pdf_page(
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

CALL add_column_if_missing_pdf_page('exam_document_analysis_chunk', 'page_no',
    'ALTER TABLE exam_document_analysis_chunk ADD COLUMN page_no INT NOT NULL DEFAULT 0 AFTER document_id');
CALL add_column_if_missing_pdf_page('exam_document_analysis_chunk', 'page_image_path',
    'ALTER TABLE exam_document_analysis_chunk ADD COLUMN page_image_path VARCHAR(512) NULL AFTER page_no');
CALL add_column_if_missing_pdf_page('exam_document_analysis_chunk', 'page_width',
    'ALTER TABLE exam_document_analysis_chunk ADD COLUMN page_width INT NULL AFTER page_image_path');
CALL add_column_if_missing_pdf_page('exam_document_analysis_chunk', 'page_height',
    'ALTER TABLE exam_document_analysis_chunk ADD COLUMN page_height INT NULL AFTER page_width');
CALL add_column_if_missing_pdf_page('exam_document_analysis_chunk', 'notified_at',
    'ALTER TABLE exam_document_analysis_chunk ADD COLUMN notified_at DATETIME NULL AFTER finished_at');

CALL run_sql_if_column_exists_pdf_page('exam_document_analysis_chunk', 'chunk_index',
    'UPDATE exam_document_analysis_chunk SET page_no = chunk_index + 1 WHERE page_no = 0');

CALL drop_index_if_exists_pdf_page('exam_document_analysis_chunk', 'uk_analysis_chunk_index');
CALL drop_index_if_exists_pdf_page('exam_document_analysis_chunk', 'idx_chunk_hash');
CALL drop_index_if_exists_pdf_page('exam_document_analysis_chunk', 'uk_analysis_page_no');

CALL drop_column_if_exists_pdf_page('exam_document', 'extracted_text');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'chunk_index');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'chunk_text');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'chunk_hash');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'start_offset');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'end_offset');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'question_count_estimate');
CALL drop_column_if_exists_pdf_page('exam_document_analysis_chunk', 'oversized');

CALL add_index_if_missing_pdf_page('exam_document_analysis_chunk', 'uk_analysis_page_no',
    'ALTER TABLE exam_document_analysis_chunk ADD UNIQUE KEY uk_analysis_page_no (analysis_id, page_no, deleted)');
CALL add_index_if_missing_pdf_page('exam_document_analysis_chunk', 'idx_chunk_document_status',
    'ALTER TABLE exam_document_analysis_chunk ADD INDEX idx_chunk_document_status (document_id, status)');

DROP PROCEDURE IF EXISTS add_column_if_missing_pdf_page;
DROP PROCEDURE IF EXISTS drop_column_if_exists_pdf_page;
DROP PROCEDURE IF EXISTS drop_index_if_exists_pdf_page;
DROP PROCEDURE IF EXISTS add_index_if_missing_pdf_page;
DROP PROCEDURE IF EXISTS run_sql_if_column_exists_pdf_page;
