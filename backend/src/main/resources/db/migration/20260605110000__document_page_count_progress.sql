-- 为文档表补充 PDF 总页数字段，供前端展示真实页级分片进度。

DROP PROCEDURE IF EXISTS add_column_if_missing_document_page_count;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_document_page_count(
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

CALL add_column_if_missing_document_page_count('exam_document', 'page_count',
    'ALTER TABLE exam_document ADD COLUMN page_count INT NOT NULL DEFAULT 0 AFTER storage_path');

UPDATE exam_document d
JOIN (
    SELECT document_id, MAX(page_no) AS max_page_no
    FROM exam_document_analysis_chunk
    WHERE deleted = 0
    GROUP BY document_id
) c ON c.document_id = d.id
SET d.page_count = c.max_page_no
WHERE d.page_count = 0;

DROP PROCEDURE IF EXISTS add_column_if_missing_document_page_count;
