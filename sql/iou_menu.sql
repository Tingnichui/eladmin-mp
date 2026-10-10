-- 电子借条后台菜单及超级管理员授权，在 eladmin 主库执行，可重复执行。
USE `eladmin`;
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time)
SELECT NULL,2,0,'电子借条','Iou','Layout',40,'documentation','iou',b'0',b'0',b'0',NULL,'admin','admin',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE title='电子借条' AND type=0);
SET @iou_parent_id := (SELECT menu_id FROM sys_menu WHERE title='电子借条' AND type=0 ORDER BY menu_id LIMIT 1);
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_parent_id,3,1,'借条用户管理','IouUser','iou/iouUser/index',1,'list','iouUser',b'0',b'0',b'0','iouUser:list','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_parent_id AND path='iouUser');
SET @iou_page_id := (SELECT menu_id FROM sys_menu WHERE pid=@iou_parent_id AND path='iouUser' ORDER BY menu_id LIMIT 1);
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_page_id,0,2,'借条用户管理新增',NULL,'',1,'','',b'0',b'0',b'0','iouUser:add','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_page_id AND permission='iouUser:add');
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_page_id,0,2,'借条用户管理编辑',NULL,'',2,'','',b'0',b'0',b'0','iouUser:edit','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_page_id AND permission='iouUser:edit');
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_page_id,0,2,'借条用户管理删除',NULL,'',3,'','',b'0',b'0',b'0','iouUser:del','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_page_id AND permission='iouUser:del');
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_parent_id,3,1,'借条管理','IouNote','iou/iouNote/index',2,'list','iouNote',b'0',b'0',b'0','iouNote:list','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_parent_id AND path='iouNote');
SET @iou_page_id := (SELECT menu_id FROM sys_menu WHERE pid=@iou_parent_id AND path='iouNote' ORDER BY menu_id LIMIT 1);
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_page_id,0,2,'借条管理新增',NULL,'',1,'','',b'0',b'0',b'0','iouNote:add','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_page_id AND permission='iouNote:add');
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_page_id,0,2,'借条管理编辑',NULL,'',2,'','',b'0',b'0',b'0','iouNote:edit','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_page_id AND permission='iouNote:edit');
INSERT INTO sys_menu (pid,sub_count,type,title,name,component,menu_sort,icon,path,i_frame,cache,hidden,permission,create_by,update_by,create_time,update_time) SELECT @iou_page_id,0,2,'借条管理删除',NULL,'',3,'','',b'0',b'0',b'0','iouNote:del','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE pid=@iou_page_id AND permission='iouNote:del');
UPDATE sys_menu m SET sub_count=(SELECT COUNT(*) FROM (SELECT pid FROM sys_menu) c WHERE c.pid=m.menu_id) WHERE m.menu_id=@iou_parent_id OR m.pid=@iou_parent_id;
SET @iou_admin_role_id := (SELECT role_id FROM sys_role WHERE name='超级管理员' AND level=1 ORDER BY role_id LIMIT 1);
INSERT IGNORE INTO sys_roles_menus (menu_id,role_id)
SELECT m.menu_id,@iou_admin_role_id FROM sys_menu m
WHERE @iou_admin_role_id IS NOT NULL AND (m.menu_id=@iou_parent_id OR m.pid=@iou_parent_id OR m.pid IN (SELECT menu_id FROM (SELECT menu_id,pid FROM sys_menu) p WHERE p.pid=@iou_parent_id));
COMMIT;
