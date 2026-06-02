INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '系统管理', '/system-management', 'MenuGroup', 'Setting', 30, 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/system-management' AND deleted = 0
);

INSERT INTO sys_menu (menu_name, path, component, icon, sort_order, status, permission_code)
SELECT '题库管理', '/question-management', 'MenuGroup', 'Collection', 20, 1, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/question-management' AND deleted = 0
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE path = '/system-management' AND deleted = 0 LIMIT 1) system_group),
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
SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE path = '/question-management' AND deleted = 0 LIMIT 1) question_group),
    menu_name = '可用题',
    path = '/questions/available',
    component = 'AvailableQuestionsPage',
    icon = 'Collection',
    sort_order = 10,
    status = 1,
    permission_code = 'question:list'
WHERE path IN ('/questions', '/questions/available')
  AND deleted = 0;

INSERT INTO sys_menu (parent_id, menu_name, path, component, icon, sort_order, status, permission_code)
SELECT g.id, '待确认题', '/questions/pending-confirm', 'PendingConfirmQuestionsPage', 'EditPen', 20, 1, 'question:review'
FROM sys_menu g
WHERE g.path = '/question-management'
  AND g.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu WHERE path = '/questions/pending-confirm' AND deleted = 0
  );
