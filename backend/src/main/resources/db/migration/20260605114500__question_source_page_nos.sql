-- 记录文档解析题目的来源页码，支持跨页题目追溯。

DROP PROCEDURE IF EXISTS add_column_if_missing_question_source_page_nos;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_question_source_page_nos(
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

CALL add_column_if_missing_question_source_page_nos('exam_question_source', 'source_page_nos',
    'ALTER TABLE exam_question_source ADD COLUMN source_page_nos VARCHAR(128) NULL AFTER chunk_id');

UPDATE exam_question_source s
JOIN exam_document_analysis_chunk c ON c.id = s.chunk_id
SET s.source_page_nos = CAST(c.page_no AS CHAR)
WHERE s.source_page_nos IS NULL
  AND s.chunk_id IS NOT NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing_question_source_page_nos;
