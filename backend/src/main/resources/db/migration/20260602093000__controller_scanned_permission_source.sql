-- 将权限数据收敛为 Controller 扫描来源，并修正菜单绑定的权限码。
-- 依赖字段仅作为普通字段或索引使用，不建立数据库外键或级联约束。

SET @sql = IF(
    (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH, 0)
     FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'sys_permission'
       AND COLUMN_NAME = 'permission_name') < 512,
    'ALTER TABLE sys_permission MODIFY COLUMN permission_name VARCHAR(512) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu
SET permission_code = CASE path
    WHEN '/admin/users' THEN 'admin:user:list'
    WHEN '/admin/roles' THEN 'admin:role:list'
    WHEN '/admin/permissions' THEN 'admin:permission:list'
    WHEN '/admin/menus' THEN 'admin:menu:list'
    WHEN '/system-configs' THEN 'system-config:list'
    WHEN '/notifications' THEN 'notification:list'
    ELSE permission_code
END
WHERE deleted = 0
  AND path IN (
      '/admin/users',
      '/admin/roles',
      '/admin/permissions',
      '/admin/menus',
      '/system-configs',
      '/notifications'
  );

UPDATE sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
SET rp.deleted = rp.id
WHERE rp.deleted = 0
  AND p.deleted = 0
  AND (
      p.permission_code LIKE '%:page'
      OR p.permission_code LIKE '\\_\\_menu:%' ESCAPE '\\'
      OR p.permission_code = '__uncategorized'
      OR p.permission_type IN ('MENU', 'VIEW')
      OR p.system_generated = 0
      OR p.last_scanned_at IS NULL
  );

UPDATE sys_permission p
SET p.deleted = p.id
WHERE p.deleted = 0
  AND (
      p.permission_code LIKE '%:page'
      OR p.permission_code LIKE '\\_\\_menu:%' ESCAPE '\\'
      OR p.permission_code = '__uncategorized'
      OR p.permission_type IN ('MENU', 'VIEW')
      OR p.system_generated = 0
      OR p.last_scanned_at IS NULL
  );
