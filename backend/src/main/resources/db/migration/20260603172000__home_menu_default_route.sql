-- 新增首页菜单并调整登录默认菜单顺序。
-- 本迁移可重复执行：新增使用 NOT EXISTS，已有菜单只补齐开发维护字段。

INSERT INTO sys_menu (menu_key, menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT 'menu:/home', '首页', '/home', NULL, 'House', 10, 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/home' AND deleted = 0
);

UPDATE sys_menu
SET menu_key = 'menu:/home',
    menu_name = '首页',
    api_path = NULL,
    icon = 'House',
    sort_order = 10,
    permission_code = NULL
WHERE deleted = 0
  AND path = '/home';

UPDATE sys_menu
SET menu_key = 'menu:/documents',
    menu_name = '我的文档',
    api_path = '/api/documents',
    icon = 'Document',
    sort_order = 20,
    permission_code = 'document:list'
WHERE deleted = 0
  AND path = '/documents';

UPDATE sys_menu
SET sort_order = 30
WHERE deleted = 0
  AND path IS NULL
  AND menu_key = 'group:/questions';

UPDATE sys_menu
SET sort_order = 40
WHERE deleted = 0
  AND path IS NULL
  AND menu_key = 'group:/admin';

UPDATE sys_menu
SET sort_order = 50
WHERE deleted = 0
  AND path = '/system-configs';

UPDATE sys_menu
SET sort_order = 60
WHERE deleted = 0
  AND path = '/notifications';

UPDATE sys_menu
SET sort_order = 70
WHERE deleted = 0
  AND path = '/profile';
