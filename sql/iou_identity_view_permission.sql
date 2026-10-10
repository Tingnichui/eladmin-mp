-- 可重复执行，只为超级管理员追加身份证查看权限，其他角色按需授权。
USE `eladmin`;
SET NAMES utf8mb4;
START TRANSACTION;
SET @iou_identity_page_id := (SELECT menu_id FROM sys_menu WHERE component='iou/iouUser/index' AND type=1 ORDER BY menu_id LIMIT 1);
INSERT INTO sys_menu (pid,sub_count,type,title,menu_sort,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time)
SELECT @iou_identity_page_id,0,2,'借条用户查看身份证',4,b'0',b'0',b'0','iouUser:viewIdentity','admin','admin',NOW(),NOW()
WHERE @iou_identity_page_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission='iouUser:viewIdentity');
UPDATE sys_menu SET sub_count=(SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) c WHERE c.pid=@iou_identity_page_id) WHERE menu_id=@iou_identity_page_id;
INSERT IGNORE INTO sys_roles_menus (menu_id,role_id)
SELECT m.menu_id,r.role_id FROM sys_menu m JOIN sys_role r ON r.name='超级管理员' AND r.level=1 WHERE m.permission='iouUser:viewIdentity';
COMMIT;
