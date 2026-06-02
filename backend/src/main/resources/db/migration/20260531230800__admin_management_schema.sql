-- 初始化后台管理扩展字段、菜单表和管理权限。
-- 依赖字段仅作为普通字段或索引使用，不建立数据库外键或级联约束。

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'force_password_change') = 0,
    'ALTER TABLE sys_user ADD COLUMN force_password_change TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password_updated_at') = 0,
    'ALTER TABLE sys_user ADD COLUMN password_updated_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    parent_id BIGINT NULL,
    menu_name VARCHAR(64) NOT NULL,
    path VARCHAR(128) NULL,
    api_path VARCHAR(128) NULL,
    icon VARCHAR(64) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    permission_code VARCHAR(128) NULL,
    UNIQUE KEY uk_sys_menu_path (path, deleted),
    KEY idx_sys_menu_parent_sort (parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS add_column_if_missing_admin;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_admin(
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

CALL add_column_if_missing_admin('sys_menu', 'create_id', 'ALTER TABLE sys_menu ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing_admin('sys_menu', 'create_time', 'ALTER TABLE sys_menu ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing_admin('sys_menu', 'update_id', 'ALTER TABLE sys_menu ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing_admin('sys_menu', 'update_time', 'ALTER TABLE sys_menu ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing_admin('sys_menu', 'deleted', 'ALTER TABLE sys_menu ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');
CALL add_column_if_missing_admin('sys_menu', 'api_path', 'ALTER TABLE sys_menu ADD COLUMN api_path VARCHAR(128) NULL AFTER path');

DROP PROCEDURE IF EXISTS add_column_if_missing_admin;

SET @sql = IF(
    (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'path') = 'NO',
    'ALTER TABLE sys_menu MODIFY COLUMN path VARCHAR(128) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'component') > 0,
    'ALTER TABLE sys_menu DROP COLUMN component',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '我的文档', '/documents', '/api/documents', 'Document', 10, 1, 'document:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/documents' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '我的题库', '/questions', '/api/questions', 'Collection', 20, 1, 'question:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/questions' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '用户管理', '/admin/users', '/api/admin/users', 'User', 30, 1, 'admin:user:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/users' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '角色管理', '/admin/roles', '/api/admin/roles', 'UserFilled', 40, 1, 'admin:role:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/roles' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '权限管理', '/admin/permissions', '/api/admin/permissions', 'Key', 50, 1, 'admin:permission:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/permissions' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '菜单管理', '/admin/menus', '/api/admin/menus', 'Menu', 60, 1, 'admin:menu:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/menus' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '系统配置', '/system-configs', '/api/system-configs', 'Setting', 70, 1, 'system-config:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system-configs' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '站内通知', '/notifications', '/api/notifications', 'Bell', 80, 1, 'notification:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/notifications' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '用户详情', '/profile', NULL, 'User', 90, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/profile' AND deleted = 0);
