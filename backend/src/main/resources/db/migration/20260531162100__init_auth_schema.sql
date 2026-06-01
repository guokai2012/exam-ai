CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    revoked_at DATETIME NULL,
    replaced_by_hash VARCHAR(128) NULL,
    created_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_user_session (user_id, session_id),
    KEY idx_refresh_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
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
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'auth:logout', 'auth:me', 'password:change'
)
WHERE r.role_code = 'STUDENT'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
