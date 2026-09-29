-- 为既有 ELADMIN 代码生成器元数据增加数据源维度。
-- 在 eladmin 数据库中执行；脚本可重复执行。

SET @current_schema = DATABASE();

SET @sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'code_column'
      AND column_name = 'data_source'
  ),
  'SELECT 1',
  'ALTER TABLE `code_column` ADD COLUMN `data_source` varchar(64) NOT NULL DEFAULT ''master'' COMMENT ''数据源名称'' AFTER `column_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = @current_schema
      AND table_name = 'code_column'
      AND index_name = 'idx_data_source_table_name'
  ),
  'SELECT 1',
  'ALTER TABLE `code_column` ADD INDEX `idx_data_source_table_name` (`data_source`, `table_name`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @current_schema
      AND table_name = 'code_config'
      AND column_name = 'data_source'
  ),
  'SELECT 1',
  'ALTER TABLE `code_config` ADD COLUMN `data_source` varchar(64) NOT NULL DEFAULT ''master'' COMMENT ''数据源名称'' AFTER `config_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = @current_schema
      AND table_name = 'code_config'
      AND index_name = 'idx_data_source_table_name'
  ),
  'SELECT 1',
  'ALTER TABLE `code_config` ADD INDEX `idx_data_source_table_name` (`data_source`, `table_name`(100))'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
