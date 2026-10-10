-- 策略告警事件 CRUD 菜单，放在“投资策略”目录下；在 eladmin 主库执行，脚本可重复执行。
START TRANSACTION;

SET @investment_strategy_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE title = '投资策略' AND type = 0
    ORDER BY menu_id
    LIMIT 1
);

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT
    @investment_strategy_menu_id, 3, 1, '策略告警事件', 'ResearchStrategyAlertEvents',
    'invest/researchStrategyAlertEvents/index', 1, 'warning', 'researchStrategyAlertEvents',
    b'0', b'0', b'0', 'researchStrategyAlertEvents:list', 'admin', 'admin', NOW(), NOW()
WHERE @investment_strategy_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu
      WHERE pid = @investment_strategy_menu_id AND path = 'researchStrategyAlertEvents'
  );

SET @strategy_alert_events_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE pid = @investment_strategy_menu_id AND path = 'researchStrategyAlertEvents'
    ORDER BY menu_id
    LIMIT 1
);

UPDATE sys_menu
SET title = '策略告警事件',
    name = 'ResearchStrategyAlertEvents',
    component = 'invest/researchStrategyAlertEvents/index',
    menu_sort = 1,
    icon = 'warning',
    type = 1,
    permission = 'researchStrategyAlertEvents:list',
    sub_count = 3,
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = @strategy_alert_events_menu_id;

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT @strategy_alert_events_menu_id, 0, 2, '策略告警事件新增', NULL, '', 2, '', '',
       b'0', b'0', b'0', 'researchStrategyAlertEvents:add', 'admin', 'admin', NOW(), NOW()
WHERE @strategy_alert_events_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'researchStrategyAlertEvents:add');

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT @strategy_alert_events_menu_id, 0, 2, '策略告警事件编辑', NULL, '', 3, '', '',
       b'0', b'0', b'0', 'researchStrategyAlertEvents:edit', 'admin', 'admin', NOW(), NOW()
WHERE @strategy_alert_events_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'researchStrategyAlertEvents:edit');

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT @strategy_alert_events_menu_id, 0, 2, '策略告警事件删除', NULL, '', 4, '', '',
       b'0', b'0', b'0', 'researchStrategyAlertEvents:del', 'admin', 'admin', NOW(), NOW()
WHERE @strategy_alert_events_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'researchStrategyAlertEvents:del');

UPDATE sys_menu parent
SET parent.sub_count = (
    SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) child WHERE child.pid = parent.menu_id
)
WHERE parent.menu_id IN (@investment_strategy_menu_id, @strategy_alert_events_menu_id);

INSERT IGNORE INTO sys_roles_menus (menu_id, role_id)
SELECT child.menu_id, parent_role.role_id
FROM sys_menu child
JOIN sys_roles_menus parent_role ON parent_role.menu_id = @investment_strategy_menu_id
WHERE child.menu_id = @strategy_alert_events_menu_id
   OR child.pid = @strategy_alert_events_menu_id;

COMMIT;
