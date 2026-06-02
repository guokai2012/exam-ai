-- 重组导航菜单，分组菜单使用 path = NULL，不允许绑定 api_path。
-- 菜单结构由开发脚本维护，管理员仅维护展示字段。

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '系统管理', NULL, NULL, 'Setting', 30, 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE menu_name = '系统管理'
      AND parent_id IS NULL
      AND deleted = 0
);

INSERT INTO sys_menu (menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT '题库管理', NULL, NULL, 'Collection', 20, 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE menu_name = '题库管理'
      AND parent_id IS NULL
      AND deleted = 0
);

UPDATE sys_menu
SET path = NULL,
    api_path = NULL,
    permission_code = NULL
WHERE deleted = 0
  AND path IN ('/system-management', '/question-management');

UPDATE sys_menu
SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id IS NULL AND path IS NULL AND deleted = 0 LIMIT 1) system_group),
    api_path = CASE path
        WHEN '/admin/users' THEN '/api/admin/users'
        WHEN '/admin/roles' THEN '/api/admin/roles'
        WHEN '/admin/permissions' THEN '/api/admin/permissions'
        WHEN '/admin/menus' THEN '/api/admin/menus'
        WHEN '/system-configs' THEN '/api/system-configs'
        ELSE api_path
    END,
    sort_order = CASE path
        WHEN '/admin/users' THEN 10
        WHEN '/admin/roles' THEN 20
        WHEN '/admin/permissions' THEN 30
        WHEN '/admin/menus' THEN 40
        WHEN '/system-configs' THEN 50
        ELSE sort_order
    END
WHERE path IN ('/admin/users', '/admin/roles', '/admin/permissions', '/admin/menus', '/system-configs')
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE menu_name = '题库管理' AND parent_id IS NULL AND path IS NULL AND deleted = 0 LIMIT 1) question_group),
    menu_name = '可用题',
    path = '/questions/available',
    api_path = '/api/questions',
    icon = 'Collection',
    sort_order = 10,
    status = 1,
    permission_code = 'question:list'
WHERE path IN ('/questions', '/questions/available')
  AND deleted = 0;

INSERT INTO sys_menu (parent_id, menu_name, path, api_path, icon, sort_order, status, permission_code)
SELECT g.id, '待确认题', '/questions/pending-confirm', '/api/questions', 'EditPen', 20, 1, 'question:review'
FROM sys_menu g
WHERE g.menu_name = '题库管理'
  AND g.parent_id IS NULL
  AND g.path IS NULL
  AND g.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu WHERE path = '/questions/pending-confirm' AND deleted = 0
  );

UPDATE sys_menu
SET api_path = NULL
WHERE path IS NULL
  AND deleted = 0;
