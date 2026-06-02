SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'parent_id') = 0,
    'ALTER TABLE sys_permission ADD COLUMN parent_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'menu_id') = 0,
    'ALTER TABLE sys_permission ADD COLUMN menu_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'permission_type') = 0,
    'ALTER TABLE sys_permission ADD COLUMN permission_type VARCHAR(32) NOT NULL DEFAULT ''ACTION''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'sort_order') = 0,
    'ALTER TABLE sys_permission ADD COLUMN sort_order INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'system_generated') = 0,
    'ALTER TABLE sys_permission ADD COLUMN system_generated TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'last_scanned_at') = 0,
    'ALTER TABLE sys_permission ADD COLUMN last_scanned_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND INDEX_NAME = 'idx_sys_permission_parent_sort') = 0,
    'ALTER TABLE sys_permission ADD KEY idx_sys_permission_parent_sort (parent_id, sort_order)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND INDEX_NAME = 'idx_sys_permission_menu') = 0,
    'ALTER TABLE sys_permission ADD KEY idx_sys_permission_menu (menu_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_permission (
    parent_id,
    menu_id,
    permission_code,
    permission_name,
    permission_type,
    sort_order,
    system_generated
)
SELECT NULL, NULL, '__uncategorized', '未归类权限', 'GROUP', 9999, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = '__uncategorized' AND deleted = 0
);

INSERT INTO sys_permission (
    parent_id,
    menu_id,
    permission_code,
    permission_name,
    permission_type,
    sort_order,
    system_generated
)
SELECT NULL,
       m.id,
       CONCAT('__menu:', m.id),
       m.menu_name,
       CASE WHEN m.component = 'MenuGroup' THEN 'GROUP' ELSE 'MENU' END,
       m.sort_order,
       1
FROM sys_menu m
WHERE m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('__menu:', m.id) AND p.deleted = 0
);

UPDATE sys_permission p
JOIN sys_menu m ON p.permission_code = CONCAT('__menu:', m.id) AND m.deleted = 0
LEFT JOIN sys_permission parent_permission ON parent_permission.permission_code = CONCAT('__menu:', m.parent_id)
    AND parent_permission.deleted = 0
SET p.parent_id = parent_permission.id,
    p.menu_id = m.id,
    p.permission_name = m.menu_name,
    p.permission_type = CASE WHEN m.component = 'MenuGroup' THEN 'GROUP' ELSE 'MENU' END,
    p.sort_order = m.sort_order,
    p.system_generated = 1
WHERE p.deleted = 0;

INSERT INTO sys_permission (
    parent_id,
    menu_id,
    permission_code,
    permission_name,
    permission_type,
    sort_order,
    system_generated
)
SELECT menu_permission.id,
       m.id,
       m.permission_code,
       '查看',
       'VIEW',
       0,
       1
FROM sys_menu m
JOIN sys_permission menu_permission ON menu_permission.permission_code = CONCAT('__menu:', m.id)
    AND menu_permission.deleted = 0
WHERE m.deleted = 0
  AND m.permission_code IS NOT NULL
  AND m.permission_code <> ''
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission p WHERE p.permission_code = m.permission_code AND p.deleted = 0
  );

UPDATE sys_permission p
JOIN sys_menu m ON m.permission_code = p.permission_code AND m.deleted = 0
JOIN sys_permission menu_permission ON menu_permission.permission_code = CONCAT('__menu:', m.id)
    AND menu_permission.deleted = 0
SET p.parent_id = menu_permission.id,
    p.menu_id = m.id,
    p.permission_type = 'VIEW',
    p.sort_order = 0,
    p.system_generated = 1
WHERE p.deleted = 0
  AND m.permission_code IS NOT NULL
  AND m.permission_code <> '';

UPDATE sys_permission p
JOIN sys_permission uncategorized ON uncategorized.permission_code = '__uncategorized'
    AND uncategorized.deleted = 0
SET p.parent_id = uncategorized.id
WHERE p.deleted = 0
  AND p.parent_id IS NULL
  AND p.permission_type = 'ACTION'
  AND p.permission_code <> '__uncategorized';
