-- 现货底仓 CRUD 菜单，放在“投资管理”下；脚本可重复执行。
START TRANSACTION;

SET @invest_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE title = '投资管理' AND type = 0
    ORDER BY menu_id
    LIMIT 1
);

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT
    @invest_menu_id, 3, 1, '现货底仓', 'BinanceSpotCorePosition',
    'invest/binanceSpotCorePosition/index', 83, 'lock', 'binanceSpotCorePosition',
    b'0', b'0', b'0', 'binanceSpotCorePosition:list', 'admin', 'admin', NOW(), NOW()
WHERE @invest_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu
      WHERE pid = @invest_menu_id AND path = 'binanceSpotCorePosition'
  );

SET @core_position_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE pid = @invest_menu_id AND path = 'binanceSpotCorePosition'
    ORDER BY menu_id
    LIMIT 1
);

UPDATE sys_menu
SET title = '现货底仓',
    name = 'BinanceSpotCorePosition',
    component = 'invest/binanceSpotCorePosition/index',
    menu_sort = 83,
    icon = 'lock',
    type = 1,
    permission = 'binanceSpotCorePosition:list',
    sub_count = 3,
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = @core_position_menu_id;

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT @core_position_menu_id, 0, 2, '现货底仓新增', NULL, '', 2, '', '',
       b'0', b'0', b'0', 'binanceSpotCorePosition:add', 'admin', 'admin', NOW(), NOW()
WHERE @core_position_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'binanceSpotCorePosition:add');

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT @core_position_menu_id, 0, 2, '现货底仓编辑', NULL, '', 3, '', '',
       b'0', b'0', b'0', 'binanceSpotCorePosition:edit', 'admin', 'admin', NOW(), NOW()
WHERE @core_position_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'binanceSpotCorePosition:edit');

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT @core_position_menu_id, 0, 2, '现货底仓删除', NULL, '', 4, '', '',
       b'0', b'0', b'0', 'binanceSpotCorePosition:del', 'admin', 'admin', NOW(), NOW()
WHERE @core_position_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'binanceSpotCorePosition:del');

UPDATE sys_menu parent
SET parent.sub_count = (
    SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) child WHERE child.pid = parent.menu_id
)
WHERE parent.menu_id IN (@invest_menu_id, @core_position_menu_id);

INSERT IGNORE INTO sys_roles_menus (menu_id, role_id)
SELECT child.menu_id, parent_role.role_id
FROM sys_menu child
JOIN sys_roles_menus parent_role ON parent_role.menu_id = @invest_menu_id
WHERE child.menu_id = @core_position_menu_id OR child.pid = @core_position_menu_id;

COMMIT;
