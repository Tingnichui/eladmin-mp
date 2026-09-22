-- 币本位合约统计菜单，放在“投资管理”下；脚本可重复执行。
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
    @invest_menu_id, 0, 1, '币本位统计', 'BinanceCoinFuturesStats',
    'invest/binance/coinFuturesStats/index', 84, 'chart', 'binanceCoinFuturesStats',
    b'0', b'0', b'0', 'binanceCoinFuturesTradeInfo:list', 'admin', 'admin', NOW(), NOW()
WHERE @invest_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu
      WHERE pid = @invest_menu_id AND path = 'binanceCoinFuturesStats'
  );

SET @coin_stats_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE pid = @invest_menu_id AND path = 'binanceCoinFuturesStats'
    ORDER BY menu_id
    LIMIT 1
);

UPDATE sys_menu
SET title = '币本位统计',
    name = 'BinanceCoinFuturesStats',
    component = 'invest/binance/coinFuturesStats/index',
    menu_sort = 84,
    icon = 'chart',
    type = 1,
    permission = 'binanceCoinFuturesTradeInfo:list',
    sub_count = 0,
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = @coin_stats_menu_id;

INSERT INTO sys_menu
    (pid, sub_count, type, title, name, component, menu_sort, icon, path,
     i_frame, cache, hidden, permission, create_by, update_by, create_time, update_time)
SELECT
    @coin_stats_menu_id, 0, 2, '币本位下单', NULL, '', 1, '', '',
    b'0', b'0', b'0', 'binanceCoinFuturesTradeInfo:order', 'admin', 'admin', NOW(), NOW()
WHERE @coin_stats_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu
      WHERE pid = @coin_stats_menu_id
        AND permission = 'binanceCoinFuturesTradeInfo:order'
  );

SET @coin_order_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE pid = @coin_stats_menu_id
      AND permission = 'binanceCoinFuturesTradeInfo:order'
    ORDER BY menu_id
    LIMIT 1
);

INSERT IGNORE INTO sys_roles_menus (menu_id, role_id)
SELECT @coin_order_menu_id, menu_role.role_id
FROM sys_roles_menus menu_role
WHERE menu_role.menu_id = @coin_stats_menu_id
  AND @coin_order_menu_id IS NOT NULL;

UPDATE sys_menu parent
SET parent.sub_count = (
    SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) child WHERE child.pid = parent.menu_id
)
WHERE parent.menu_id = @coin_stats_menu_id;

UPDATE sys_menu parent
SET parent.sub_count = (
    SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) child WHERE child.pid = parent.menu_id
)
WHERE parent.menu_id = @invest_menu_id;

INSERT IGNORE INTO sys_roles_menus (menu_id, role_id)
SELECT @coin_stats_menu_id, parent_role.role_id
FROM sys_roles_menus parent_role
WHERE parent_role.menu_id = @invest_menu_id
  AND @coin_stats_menu_id IS NOT NULL;

COMMIT;
