-- 新数据库已初始化 eladmin.sql、iou_user.sql 时使用，仅执行一次。
-- 若已经执行 iou_registration_migration.sql，不得重复执行本文件。
-- 固定ID冲突必须停止并核查，不使用INSERT IGNORE或覆盖已有部门/岗位/角色。
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO sys_dept (dept_id,pid,name,enabled,dept_sort,create_by,create_time)
VALUES (920001,NULL,'借条用户',b'1',999,'System',NOW());
INSERT INTO sys_job (job_id,name,enabled,job_sort,create_by,create_time)
VALUES (920001,'借条用户',b'1',999,'System',NOW());
INSERT INTO sys_role (role_id,name,level,data_scope,description,create_by,create_time)
VALUES (920001,'借条用户',999,'本级','仅按需授权H5本人接口，不授予后台权限','System',NOW());
INSERT INTO sys_menu (menu_id,pid,type,title,name,permission,hidden,i_frame,cache,menu_sort,create_by,create_time)
VALUES (920001,NULL,2,'H5查看本人资料',NULL,'iou:profile:read',b'1',b'0',b'0',999,'System',NOW());
INSERT INTO sys_roles_menus (menu_id,role_id) VALUES (920001,920001);
COMMIT;
