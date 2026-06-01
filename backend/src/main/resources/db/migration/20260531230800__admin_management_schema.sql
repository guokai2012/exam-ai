ALTER TABLE sys_user
    ADD COLUMN force_password_change TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN password_updated_at DATETIME NULL;

CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    menu_name VARCHAR(64) NOT NULL,
    path VARCHAR(128) NOT NULL,
    component VARCHAR(128) NOT NULL,
    icon VARCHAR(64) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    permission_code VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sys_menu_parent_sort (parent_id, sort_order),
    CONSTRAINT fk_sys_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'password:change'
WHERE r.role_code IN ('TEACHER', 'STUDENT')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code) VALUES
('我的文档', '/documents', 'DocumentsPage', 'Document', 10, 1, 'document:list'),
('我的题库', '/questions', 'QuestionsPage', 'Collection', 20, 1, 'question:list'),
('用户管理', '/admin/users', 'AdminUsersPage', 'User', 30, 1, 'admin:user:page'),
('角色管理', '/admin/roles', 'AdminRolesPage', 'UserFilled', 40, 1, 'admin:role:page'),
('权限管理', '/admin/permissions', 'AdminPermissionsPage', 'Key', 50, 1, 'admin:permission:page'),
('菜单管理', '/admin/menus', 'AdminMenusPage', 'Menu', 60, 1, 'admin:menu:page'),
('系统配置', '/system-configs', 'SystemConfigPage', 'Setting', 70, 1, 'system-config:page'),
('站内通知', '/notifications', 'NotificationsPage', 'Bell', 80, 1, 'notification:page'),
('用户详情', '/profile', 'ProfilePage', 'User', 90, 1, NULL);
