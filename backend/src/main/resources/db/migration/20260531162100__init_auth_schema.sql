-- 初始化认证、角色、权限与令牌表。
-- 依赖字段仅作为普通字段或索引使用，不建立数据库外键或级联约束。

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    force_password_change TINYINT(1) NOT NULL DEFAULT 0,
    last_login_at DATETIME NULL,
    password_updated_at DATETIME NULL,
    UNIQUE KEY uk_sys_user_username (username, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_sys_role_code (role_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    parent_id BIGINT NULL,
    menu_id BIGINT NULL,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    permission_type VARCHAR(32) NOT NULL DEFAULT 'ACTION',
    sort_order INT NOT NULL DEFAULT 0,
    system_generated TINYINT(1) NOT NULL DEFAULT 0,
    last_scanned_at DATETIME NULL,
    UNIQUE KEY uk_sys_permission_code (permission_code, deleted),
    KEY idx_sys_permission_parent_sort (parent_id, sort_order),
    KEY idx_sys_permission_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_sys_user_role_pair (user_id, role_id, deleted),
    KEY idx_sys_user_role_user (user_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_sys_role_permission_pair (role_id, permission_id, deleted),
    KEY idx_sys_role_permission_role (role_id),
    KEY idx_sys_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    create_id BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_id BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    token_hash VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    revoked_at DATETIME NULL,
    replaced_by_hash VARCHAR(128) NULL,
    created_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    UNIQUE KEY uk_refresh_token_hash (token_hash, deleted),
    KEY idx_refresh_user_session (user_id, session_id),
    KEY idx_refresh_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS add_column_if_missing_auth;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_auth(
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

CALL add_column_if_missing_auth('sys_user', 'create_id', 'ALTER TABLE sys_user ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing_auth('sys_user', 'create_time', 'ALTER TABLE sys_user ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing_auth('sys_user', 'update_id', 'ALTER TABLE sys_user ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing_auth('sys_user', 'update_time', 'ALTER TABLE sys_user ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing_auth('sys_user', 'deleted', 'ALTER TABLE sys_user ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');
CALL add_column_if_missing_auth('sys_user', 'force_password_change', 'ALTER TABLE sys_user ADD COLUMN force_password_change TINYINT(1) NOT NULL DEFAULT 0');
CALL add_column_if_missing_auth('sys_user', 'password_updated_at', 'ALTER TABLE sys_user ADD COLUMN password_updated_at DATETIME NULL');

CALL add_column_if_missing_auth('sys_role', 'create_id', 'ALTER TABLE sys_role ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing_auth('sys_role', 'create_time', 'ALTER TABLE sys_role ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing_auth('sys_role', 'update_id', 'ALTER TABLE sys_role ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing_auth('sys_role', 'update_time', 'ALTER TABLE sys_role ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing_auth('sys_role', 'deleted', 'ALTER TABLE sys_role ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing_auth('sys_permission', 'create_id', 'ALTER TABLE sys_permission ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing_auth('sys_permission', 'create_time', 'ALTER TABLE sys_permission ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing_auth('sys_permission', 'update_id', 'ALTER TABLE sys_permission ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing_auth('sys_permission', 'update_time', 'ALTER TABLE sys_permission ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing_auth('sys_permission', 'deleted', 'ALTER TABLE sys_permission ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

CALL add_column_if_missing_auth('sys_user_role', 'create_id', 'ALTER TABLE sys_user_role ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0');
CALL add_column_if_missing_auth('sys_user_role', 'create_time', 'ALTER TABLE sys_user_role ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL add_column_if_missing_auth('sys_user_role', 'update_id', 'ALTER TABLE sys_user_role ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0');
CALL add_column_if_missing_auth('sys_user_role', 'update_time', 'ALTER TABLE sys_user_role ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_missing_auth('sys_user_role', 'deleted', 'ALTER TABLE sys_user_role ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0');

CALL add_column_if_missing_auth('sys_role_permission', 'create_id', 'ALTER TABLE sys_role_permission ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0');
CALL add_column_if_missing_auth('sys_role_permission', 'create_time', 'ALTER TABLE sys_role_permission ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL add_column_if_missing_auth('sys_role_permission', 'update_id', 'ALTER TABLE sys_role_permission ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0');
CALL add_column_if_missing_auth('sys_role_permission', 'update_time', 'ALTER TABLE sys_role_permission ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_missing_auth('sys_role_permission', 'deleted', 'ALTER TABLE sys_role_permission ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0');

CALL add_column_if_missing_auth('sys_refresh_token', 'create_id', 'ALTER TABLE sys_refresh_token ADD COLUMN create_id BIGINT NOT NULL DEFAULT 0 AFTER id');
CALL add_column_if_missing_auth('sys_refresh_token', 'create_time', 'ALTER TABLE sys_refresh_token ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER create_id');
CALL add_column_if_missing_auth('sys_refresh_token', 'update_id', 'ALTER TABLE sys_refresh_token ADD COLUMN update_id BIGINT NOT NULL DEFAULT 0 AFTER create_time');
CALL add_column_if_missing_auth('sys_refresh_token', 'update_time', 'ALTER TABLE sys_refresh_token ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER update_id');
CALL add_column_if_missing_auth('sys_refresh_token', 'deleted', 'ALTER TABLE sys_refresh_token ADD COLUMN deleted BIGINT NOT NULL DEFAULT 0 AFTER update_time');

DROP PROCEDURE IF EXISTS add_column_if_missing_auth;

INSERT INTO sys_role (role_code, role_name) VALUES
('ADMIN', '管理员'),
('TEACHER', '教师'),
('STUDENT', '学生')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO sys_permission (permission_code, permission_name) VALUES
('admin:user:page', '用户管理页面'),
('admin:user:list', '查询用户列表'),
('admin:user:create', '新建用户'),
('admin:user:update', '编辑用户'),
('admin:user:disable', '禁用用户'),
('admin:user:reset-password', '重置用户密码'),
('auth:logout', '退出登录'),
('auth:me', '查询当前用户'),
('password:change', '修改密码'),
('question-category:list', '查询题目分类'),
('question-category:create', '新建题目分类'),
('question:list', '查询题目列表'),
('question:detail', '查询题目详情'),
('question:review', '审核题目')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'admin:user:page', 'admin:user:list', 'admin:user:create',
    'admin:user:update', 'admin:user:disable', 'admin:user:reset-password',
    'auth:logout', 'auth:me', 'password:change'
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
JOIN sys_permission p ON p.permission_code IN (
    'auth:logout', 'auth:me', 'password:change',
    'question-category:list', 'question-category:create',
    'question:list', 'question:detail', 'question:review'
)
WHERE r.role_code = 'TEACHER'
  AND r.deleted = 0
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'auth:logout', 'auth:me', 'password:change'
)
WHERE r.role_code = 'STUDENT'
  AND r.deleted = 0
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );
