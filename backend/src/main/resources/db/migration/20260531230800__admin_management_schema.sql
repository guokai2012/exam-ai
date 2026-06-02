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
    path VARCHAR(128) NOT NULL,
    component VARCHAR(128) NOT NULL,
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

DROP PROCEDURE IF EXISTS add_column_if_missing_admin;

INSERT INTO sys_permission (permission_code, permission_name) VALUES
('admin:role:page', '角色管理页面'),
('admin:role:list', '查询角色列表'),
('admin:role:create', '新建角色'),
('admin:role:update', '编辑角色'),
('admin:role:delete', '删除角色'),
('admin:permission:page', '权限管理页面'),
('admin:permission:list', '查询权限树'),
('admin:permission:create', '新建权限'),
('admin:permission:scan', '扫描权限'),
('admin:permission:update', '编辑权限'),
('admin:permission:delete', '删除权限'),
('admin:menu:page', '菜单管理页面'),
('admin:menu:list', '查询菜单树'),
('admin:menu:create', '新建菜单'),
('admin:menu:update', '编辑菜单'),
('admin:menu:delete', '删除菜单'),
('session:kick', '踢用户下线'),
('password:change', '修改密码')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'admin:role:page', 'admin:role:list', 'admin:role:create', 'admin:role:update', 'admin:role:delete',
    'admin:permission:page', 'admin:permission:list', 'admin:permission:create', 'admin:permission:scan',
    'admin:permission:update', 'admin:permission:delete',
    'admin:menu:page', 'admin:menu:list', 'admin:menu:create', 'admin:menu:update', 'admin:menu:delete',
    'session:kick', 'password:change'
)
WHERE r.role_code = 'ADMIN'
  AND r.deleted = 0
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'password:change'
WHERE r.role_code IN ('TEACHER', 'STUDENT')
  AND r.deleted = 0
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '我的文档', '/documents', 'DocumentsPage', 'Document', 10, 1, 'document:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/documents' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '我的题库', '/questions', 'QuestionsPage', 'Collection', 20, 1, 'question:list'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/questions' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '用户管理', '/admin/users', 'AdminUsersPage', 'User', 30, 1, 'admin:user:page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/users' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '角色管理', '/admin/roles', 'AdminRolesPage', 'UserFilled', 40, 1, 'admin:role:page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/roles' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '权限管理', '/admin/permissions', 'AdminPermissionsPage', 'Key', 50, 1, 'admin:permission:page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/permissions' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '菜单管理', '/admin/menus', 'AdminMenusPage', 'Menu', 60, 1, 'admin:menu:page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/admin/menus' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '系统配置', '/system-configs', 'SystemConfigPage', 'Setting', 70, 1, 'system-config:page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system-configs' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '站内通知', '/notifications', 'NotificationsPage', 'Bell', 80, 1, 'notification:page'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/notifications' AND deleted = 0);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '用户详情', '/profile', 'ProfilePage', 'User', 90, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/profile' AND deleted = 0);
