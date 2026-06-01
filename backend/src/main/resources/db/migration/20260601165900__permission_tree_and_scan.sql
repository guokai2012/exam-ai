ALTER TABLE sys_permission
    ADD COLUMN parent_id BIGINT NULL,
    ADD COLUMN menu_id BIGINT NULL,
    ADD COLUMN permission_type VARCHAR(32) NOT NULL DEFAULT 'ACTION',
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
    ADD COLUMN system_generated TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN last_scanned_at DATETIME NULL,
    ADD KEY idx_sys_permission_parent_sort (parent_id, sort_order),
    ADD KEY idx_sys_permission_menu (menu_id);

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
    SELECT 1 FROM sys_permission WHERE permission_code = '__uncategorized'
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
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.permission_code = CONCAT('__menu:', m.id)
);

UPDATE sys_permission p
JOIN sys_menu m ON p.permission_code = CONCAT('__menu:', m.id)
LEFT JOIN sys_permission parent_permission ON parent_permission.permission_code = CONCAT('__menu:', m.parent_id)
SET p.parent_id = parent_permission.id,
    p.menu_id = m.id,
    p.permission_name = m.menu_name,
    p.permission_type = CASE WHEN m.component = 'MenuGroup' THEN 'GROUP' ELSE 'MENU' END,
    p.sort_order = m.sort_order,
    p.system_generated = 1;

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
WHERE m.permission_code IS NOT NULL
  AND m.permission_code <> ''
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission p WHERE p.permission_code = m.permission_code
  );

UPDATE sys_permission p
JOIN sys_menu m ON m.permission_code = p.permission_code
JOIN sys_permission menu_permission ON menu_permission.permission_code = CONCAT('__menu:', m.id)
SET p.parent_id = menu_permission.id,
    p.menu_id = m.id,
    p.permission_type = 'VIEW',
    p.sort_order = 0,
    p.system_generated = 1
WHERE m.permission_code IS NOT NULL
  AND m.permission_code <> '';

UPDATE sys_permission p
JOIN sys_permission uncategorized ON uncategorized.permission_code = '__uncategorized'
SET p.parent_id = uncategorized.id
WHERE p.parent_id IS NULL
  AND p.permission_type = 'ACTION'
  AND p.permission_code <> '__uncategorized';
