-- 币安代理状态菜单，放在“投资管理”下；脚本可重复执行。
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
    @invest_menu_id, 0, 1, '代理状态', 'BinanceProxyHealth',
    'invest/binance/proxyHealth/index', 85, 'monitor', 'binanceProxyHealth',
    b'0', b'0', b'0', 'binanceProxyHealth:list', 'admin', 'admin', NOW(), NOW()
WHERE @invest_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu
      WHERE pid = @invest_menu_id AND path = 'binanceProxyHealth'
  );

SET @proxy_health_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE pid = @invest_menu_id AND path = 'binanceProxyHealth'
    ORDER BY menu_id
    LIMIT 1
);

UPDATE sys_menu
SET title = '代理状态',
    name = 'BinanceProxyHealth',
    component = 'invest/binance/proxyHealth/index',
    menu_sort = 85,
    icon = 'monitor',
    type = 1,
    permission = 'binanceProxyHealth:list',
    sub_count = 0,
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = @proxy_health_menu_id;

INSERT IGNORE INTO sys_roles_menus (menu_id, role_id)
SELECT @proxy_health_menu_id, parent_role.role_id
FROM sys_roles_menus parent_role
WHERE parent_role.menu_id = @invest_menu_id
  AND @proxy_health_menu_id IS NOT NULL;

UPDATE sys_menu parent
SET parent.sub_count = (
    SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) child WHERE child.pid = parent.menu_id
)
WHERE parent.menu_id = @invest_menu_id;

COMMIT;
