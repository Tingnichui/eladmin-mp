-- 仅适用于没有旧借条账号的环境。执行前必须检查 iou_user 数量为 0。
-- 若已有账号，需先迁移 BCrypt 密码到 sys_user 并建立一对一映射，禁止直接执行。
-- 只执行一次；表位于当前选中的数据库。固定ID冲突应停止，禁止覆盖已有对象。
SET NAMES utf8mb4;
SET @iou_migration_guard = IF((SELECT COUNT(*) FROM iou_user) = 0,
  'DO 0', 'SELECT * FROM iou_registration_requires_manual_account_migration');
PREPARE iou_migration_check FROM @iou_migration_guard;
EXECUTE iou_migration_check;
DEALLOCATE PREPARE iou_migration_check;
ALTER TABLE sys_user ADD COLUMN user_type varchar(16) NOT NULL DEFAULT 'BACKEND' COMMENT 'BACKEND后台账号/IOU借条账号';
ALTER TABLE iou_user
  DROP COLUMN password_hash,
  MODIFY mobile varchar(11) DEFAULT NULL COMMENT '短信验证绑定后方可启用登录',
  ADD COLUMN sys_user_id bigint NOT NULL COMMENT '唯一关联sys_user',
  ADD COLUMN id_verify_status tinyint NOT NULL DEFAULT 0 COMMENT '二要素状态：0未认证、2已认证',
  ADD COLUMN id_verified_at datetime DEFAULT NULL,
  ADD COLUMN agreement_version varchar(64) DEFAULT NULL,
  ADD COLUMN agreement_accepted_at datetime DEFAULT NULL,
  ADD UNIQUE KEY uk_iou_user_sys_user_id (sys_user_id),
  ADD CONSTRAINT fk_iou_user_sys_user FOREIGN KEY (sys_user_id) REFERENCES sys_user(user_id) ON DELETE RESTRICT;
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
