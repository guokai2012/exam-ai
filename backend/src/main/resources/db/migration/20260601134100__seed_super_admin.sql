INSERT INTO sys_user (
    username,
    password_hash,
    nickname,
    status,
    force_password_change,
    password_updated_at
) VALUES (
    'admin',
    '$2a$10$iKXi80kf49HAAJ.AQ/WoYuYTwZ3SOxDzKNGoFgwfB7RK.CUCoYLku',
    '超级管理员',
    1,
    0,
    NOW()
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname),
    status = VALUES(status),
    force_password_change = VALUES(force_password_change),
    password_updated_at = VALUES(password_updated_at);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );
