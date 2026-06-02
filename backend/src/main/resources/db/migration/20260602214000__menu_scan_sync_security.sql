-- 菜单扫描同步安全配置与稳定菜单标识。
-- 依赖字段仅作为普通字段或索引使用，不建立数据库外键或级联约束。

DROP PROCEDURE IF EXISTS add_column_if_missing_menu_scan;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_menu_scan(
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

CALL add_column_if_missing_menu_scan('sys_menu', 'menu_key', 'ALTER TABLE sys_menu ADD COLUMN menu_key VARCHAR(128) NULL AFTER parent_id');

DROP PROCEDURE IF EXISTS add_column_if_missing_menu_scan;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sys_menu'
       AND INDEX_NAME = 'idx_sys_menu_key') = 0,
    'ALTER TABLE sys_menu ADD INDEX idx_sys_menu_key (menu_key, deleted)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu
SET menu_key = CASE path
    WHEN '/documents' THEN 'menu:/documents'
    WHEN '/questions/available' THEN 'menu:/questions/available'
    WHEN '/questions/pending-confirm' THEN 'menu:/questions/pending-confirm'
    WHEN '/admin/users' THEN 'menu:/admin/users'
    WHEN '/admin/roles' THEN 'menu:/admin/roles'
    WHEN '/admin/permissions' THEN 'menu:/admin/permissions'
    WHEN '/admin/menus' THEN 'menu:/admin/menus'
    WHEN '/system-configs' THEN 'menu:/system-configs'
    WHEN '/notifications' THEN 'menu:/notifications'
    WHEN '/profile' THEN 'menu:/profile'
    ELSE menu_key
END
WHERE deleted = 0
  AND path IN (
      '/documents',
      '/questions/available',
      '/questions/pending-confirm',
      '/admin/users',
      '/admin/roles',
      '/admin/permissions',
      '/admin/menus',
      '/system-configs',
      '/notifications',
      '/profile'
  )
  AND menu_key IS NULL;

UPDATE sys_menu
SET menu_key = 'group:/questions'
WHERE deleted = 0
  AND path IS NULL
  AND menu_name = '题库管理'
  AND menu_key IS NULL;

UPDATE sys_menu
SET menu_key = 'group:/admin'
WHERE deleted = 0
  AND path IS NULL
  AND menu_name IN ('后台管理', '系统管理')
  AND menu_key IS NULL;

INSERT INTO sys_config (config_key, config_value, config_name, description, value_type)
SELECT 'menu.scan-token.ttl-seconds',
       '30',
       '菜单扫描临时 Token 有效期',
       '菜单管理扫描同步前必须获取短时 Token，本配置控制 Token 有效期，单位秒，最大 180 秒。',
       'INTEGER'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'menu.scan-token.ttl-seconds' AND deleted = 0
);
