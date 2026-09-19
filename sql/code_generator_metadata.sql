-- ELADMIN 代码生成器元数据表。
-- 可重复执行；仅在表不存在时创建，不删除或覆盖已有生成配置。

CREATE TABLE IF NOT EXISTS `code_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `table_name` varchar(180) DEFAULT NULL COMMENT '表名',
  `column_name` varchar(255) DEFAULT NULL COMMENT '数据库字段名称',
  `column_type` varchar(255) DEFAULT NULL COMMENT '数据库字段类型',
  `dict_name` varchar(255) DEFAULT NULL COMMENT '字典名称',
  `extra` varchar(255) DEFAULT NULL COMMENT '字段额外的参数',
  `form_show` bit(1) DEFAULT NULL COMMENT '是否表单显示',
  `form_type` varchar(255) DEFAULT NULL COMMENT '表单类型',
  `key_type` varchar(255) DEFAULT NULL COMMENT '数据库字段键类型',
  `list_show` bit(1) DEFAULT NULL COMMENT '是否在列表显示',
  `not_null` bit(1) DEFAULT NULL COMMENT '是否必填',
  `query_type` varchar(255) DEFAULT NULL COMMENT '查询类型',
  `remark` varchar(255) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`column_id`),
  KEY `idx_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='代码生成字段信息存储';

CREATE TABLE IF NOT EXISTS `code_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `table_name` varchar(255) DEFAULT NULL COMMENT '表名',
  `author` varchar(255) DEFAULT NULL COMMENT '作者',
  `cover` bit(1) DEFAULT NULL COMMENT '是否覆盖',
  `module_name` varchar(255) DEFAULT NULL COMMENT '模块名称',
  `pack` varchar(255) DEFAULT NULL COMMENT '项目包名',
  `path` varchar(255) DEFAULT NULL COMMENT '前端页面生成路径',
  `api_path` varchar(255) DEFAULT NULL COMMENT '前端 API 生成路径',
  `prefix` varchar(255) DEFAULT NULL COMMENT '表前缀',
  `api_alias` varchar(255) DEFAULT NULL COMMENT '接口名称',
  PRIMARY KEY (`config_id`),
  KEY `idx_table_name` (`table_name`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='代码生成器配置';
