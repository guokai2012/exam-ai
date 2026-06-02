-- 菜单表移除 component 字段并补齐 api_path，适配 path = NULL 分组菜单规则。
-- 本迁移可重复执行：所有结构调整均先检查元数据，数据清洗使用幂等 UPDATE。

DROP PROCEDURE IF EXISTS add_column_if_missing_menu_api;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_menu_api(
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

CALL add_column_if_missing_menu_api('sys_menu', 'api_path', 'ALTER TABLE sys_menu ADD COLUMN api_path VARCHAR(128) NULL AFTER path');

DROP PROCEDURE IF EXISTS add_column_if_missing_menu_api;

SET @sql = IF(
    (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'path') = 'NO',
    'ALTER TABLE sys_menu MODIFY COLUMN path VARCHAR(128) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu
SET path = NULL,
    api_path = NULL,
    permission_code = NULL
WHERE deleted = 0
  AND path IN ('/system-management', '/question-management');

UPDATE sys_menu
SET api_path = CASE path
    WHEN '/documents' THEN '/api/documents'
    WHEN '/questions/available' THEN '/api/questions'
    WHEN '/questions/pending-confirm' THEN '/api/questions'
    WHEN '/admin/users' THEN '/api/admin/users'
    WHEN '/admin/roles' THEN '/api/admin/roles'
    WHEN '/admin/permissions' THEN '/api/admin/permissions'
    WHEN '/admin/menus' THEN '/api/admin/menus'
    WHEN '/system-configs' THEN '/api/system-configs'
    WHEN '/notifications' THEN '/api/notifications'
    ELSE api_path
END
WHERE deleted = 0
  AND path IS NOT NULL;

UPDATE sys_menu
SET api_path = NULL
WHERE deleted = 0
  AND path IS NULL;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'component') > 0,
    'ALTER TABLE sys_menu DROP COLUMN component',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
